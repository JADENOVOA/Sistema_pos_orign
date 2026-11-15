package util;

import conexion.ConexionBD;
import dao.AuditoriaDAO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import modelo.Usuario;

/**
 * Copias de seguridad (pg_dump) y recuperación (pg_restore) de la base de datos.
 * Los respaldos se guardan con fecha y hora en la carpeta de datos del sistema.
 */
public final class RespaldoBD {

    public static final int MAX_RESPALDOS = 14;
    private static ScheduledExecutorService programador;

    private RespaldoBD() {}

    public static final class Resultado {
        public final boolean ok;
        public final File archivo;
        public final String mensaje;
        Resultado(boolean ok, File archivo, String mensaje) { this.ok = ok; this.archivo = archivo; this.mensaje = mensaje; }
    }

    public static File carpetaRespaldos() {
        String dir = Config.get("BODEGA_BACKUP_DIR", "backup.dir", null);
        File f = dir != null ? new File(dir) : Config.carpetaDatos().resolve("respaldos").toFile();
        f.mkdirs();
        return f;
    }

    // ------------------------------------------------------------------ binarios
    private static File localizar(String nombre) {
        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        String exe = win ? nombre + ".exe" : nombre;
        List<File> carpetas = new ArrayList<>();
        String cfg = Config.get("BODEGA_PG_BIN", "pg.bin", null);
        if (cfg != null) carpetas.add(new File(cfg));
        String path = System.getenv("PATH");
        if (path != null) for (String p : path.split(File.pathSeparator)) carpetas.add(new File(p));
        File[] raices = win
                ? new File[] { new File("C:\\Program Files\\PostgreSQL"), new File("C:\\Program Files (x86)\\PostgreSQL") }
                : new File[] { new File("/usr/lib/postgresql"), new File("/Library/PostgreSQL") };
        for (File raiz : raices) {
            File[] versiones = raiz.listFiles();
            if (versiones == null) continue;
            Arrays.sort(versiones, Comparator.comparing(File::getName).reversed());
            for (File v : versiones) carpetas.add(new File(v, "bin"));
        }
        for (File c : carpetas) {
            File candidato = new File(c, exe);
            if (candidato.isFile()) return candidato;
        }
        return null;
    }

    private static Resultado ejecutar(List<String> cmd, String usuario, String password, String sslmode) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            pb.environment().put("PGPASSWORD", password == null ? "" : password);
            pb.environment().put("PGSSLMODE", sslmode);
            Process p = pb.start();
            String salida = new String(p.getInputStream().readAllBytes());
            int codigo = p.waitFor();
            return new Resultado(codigo == 0, null, salida.trim());
        } catch (IOException | InterruptedException e) {
            util.Log.error("Error ejecutando " + cmd.get(0), e);
            return new Resultado(false, null, "No se pudo ejecutar la herramienta de PostgreSQL.");
        }
    }

    // ------------------------------------------------------------------ respaldo
    /** Genera un respaldo completo y lo verifica (archivo con tamaño > 0 y legible por pg_restore). */
    public static Resultado generar(Usuario u, String origen) {
        File pgDump = localizar("pg_dump");
        if (pgDump == null) {
            return new Resultado(false, null, "No se encontró pg_dump. Instale las herramientas de PostgreSQL "
                    + "o indique su carpeta en bodega.properties (pg.bin).");
        }
        String marca = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File destino = new File(carpetaRespaldos(), ConexionBD.getNombreBD() + "_" + marca + ".backup");
        List<String> cmd = Arrays.asList(pgDump.getAbsolutePath(),
                "-h", ConexionBD.getHost(), "-p", ConexionBD.getPuerto(),
                "-U", ConexionBD.getUsuario(), "-Fc", "-f", destino.getAbsolutePath(), ConexionBD.getNombreBD());
        Resultado r = ejecutar(cmd, ConexionBD.getUsuario(), ConexionBD.getPassword(), ConexionBD.getSslMode());
        if (!r.ok || !destino.isFile() || destino.length() == 0) {
            destino.delete();
            AuditoriaDAO.registrar(u, "Respaldos", "RESPALDO_FALLIDO", origen);
            return new Resultado(false, null, "No se pudo generar el respaldo.");
        }
        Resultado v = verificar(destino);
        if (!v.ok) {
            AuditoriaDAO.registrar(u, "Respaldos", "RESPALDO_FALLIDO", "Verificación incorrecta: " + destino.getName());
            return new Resultado(false, destino, "El respaldo se generó pero no pasó la verificación.");
        }
        limpiarAntiguos();
        AuditoriaDAO.registrar(u, "Respaldos", "RESPALDO_GENERADO",
                origen + ": " + destino.getName() + " (" + destino.length() + " bytes)");
        return new Resultado(true, destino, "Respaldo generado: " + destino.getName()
                + " (" + formatoTamano(destino.length()) + ")");
    }

    /** Verifica que pg_restore pueda leer el contenido del respaldo. */
    public static Resultado verificar(File archivo) {
        File pgRestore = localizar("pg_restore");
        if (pgRestore == null) return new Resultado(archivo.length() > 0, archivo, "pg_restore no encontrado");
        return ejecutar(Arrays.asList(pgRestore.getAbsolutePath(), "--list", archivo.getAbsolutePath()),
                "", "", "disable");
    }

    public static List<File> listar() {
        File[] archivos = carpetaRespaldos().listFiles((d, n) -> n.endsWith(".backup"));
        if (archivos == null) return new ArrayList<>();
        List<File> l = new ArrayList<>(Arrays.asList(archivos));
        l.sort(Comparator.comparingLong(File::lastModified).reversed());
        return l;
    }

    private static void limpiarAntiguos() {
        List<File> l = listar();
        for (int i = MAX_RESPALDOS; i < l.size(); i++) l.get(i).delete();
    }

    // ------------------------------------------------------------------ recuperación
    /**
     * Restaura un respaldo en la base indicada (se crea si no existe). Usa la cuenta administradora.
     * Para probar sin riesgo, restaure en una BD distinta (ej. BODEGA_LAPERUANITA_PRUEBA).
     */
    public static Resultado restaurar(Usuario u, File archivo, String baseDestino) {
        File pgRestore = localizar("pg_restore");
        if (pgRestore == null) return new Resultado(false, null, "No se encontró pg_restore.");
        if (!baseDestino.matches("[A-Za-z0-9_]{1,63}")) {
            return new Resultado(false, null, "Nombre de base de datos no válido (solo letras, números y _).");
        }
        try {
            crearBaseSiFalta(baseDestino);
        } catch (Exception e) {
            util.Log.error("No se pudo crear la base de destino", e);
            return new Resultado(false, null, "No se pudo preparar la base de datos de destino.");
        }
        List<String> cmd = Arrays.asList(pgRestore.getAbsolutePath(),
                "-h", ConexionBD.getHost(), "-p", ConexionBD.getPuerto(),
                "-U", ConexionBD.getUsuarioAdmin(), "-d", baseDestino,
                "--clean", "--if-exists", "--no-owner", "--exit-on-error", archivo.getAbsolutePath());
        Resultado r = ejecutar(cmd, ConexionBD.getUsuarioAdmin(), ConexionBD.getPasswordAdmin(), ConexionBD.getSslMode());
        AuditoriaDAO.registrar(u, "Respaldos", r.ok ? "RESTAURACION_OK" : "RESTAURACION_FALLIDA",
                archivo.getName() + " -> " + baseDestino);
        if (!r.ok) util.Log.info("pg_restore: " + r.mensaje);
        return new Resultado(r.ok, archivo, r.ok
                ? "Restauración completada en la base «" + baseDestino + "»."
                : "La restauración no terminó correctamente. Revise el log interno.");
    }

    private static void crearBaseSiFalta(String nombre) throws Exception {
        String url = ConexionBD.getUrl().replaceFirst("/[^/?]+(\\?|$)", "/postgres$1");
        try (Connection con = java.sql.DriverManager.getConnection(url,
                ConexionBD.getUsuarioAdmin(), ConexionBD.getPasswordAdmin())) {
            try (java.sql.PreparedStatement ps = con.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                ps.setString(1, nombre);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return;
                }
            }
            try (Statement st = con.createStatement()) {
                st.execute("CREATE DATABASE \"" + nombre + "\"");
            }
        }
    }

    // ------------------------------------------------------------------ respaldo periódico
    /** Respaldo automático: al iniciar (si el último tiene más de N horas) y luego cada N horas. */
    public static synchronized void iniciarRespaldoPeriodico(Usuario u) {
        if (programador != null) return;
        final int horas = Math.max(1, Integer.parseInt(Config.get("BODEGA_BACKUP_HOURS", "backup.hours", "24")));
        programador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "respaldo-periodico");
            t.setDaemon(true);
            return t;
        });
        List<File> actuales = listar();
        long edadHoras = actuales.isEmpty() ? Long.MAX_VALUE
                : (System.currentTimeMillis() - actuales.get(0).lastModified()) / 3_600_000L;
        long primeroSegundos = edadHoras >= horas ? 60 : (horas - edadHoras) * 3600L;
        programador.scheduleAtFixedRate(() -> {
            try { generar(u, "Respaldo automático periódico"); }
            catch (RuntimeException e) { util.Log.error("Respaldo periódico", e); }
        }, primeroSegundos, horas * 3600L, TimeUnit.SECONDS);
    }

    public static String formatoTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    public static Path rutaCarpeta() { return carpetaRespaldos().toPath(); }
    public static boolean existe(File f) { return Files.exists(f.toPath()); }
}
