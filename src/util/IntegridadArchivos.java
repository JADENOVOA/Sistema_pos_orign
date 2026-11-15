package util;

import dao.AuditoriaDAO;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import modelo.Usuario;

/**
 * Control de integridad: calcula el SHA-256 del .jar del sistema y de los archivos de configuración
 * y los compara con una línea base guardada (integridad.sha256, formato compatible con sha256sum).
 * Cualquier archivo modificado produce un hash distinto y queda detectado.
 */
public final class IntegridadArchivos {

    public enum Estado { OK, MODIFICADO, NUEVO, FALTANTE }

    public static final class Fila {
        public final String ruta, hashActual, hashBase;
        public final Estado estado;
        Fila(String ruta, String hashActual, String hashBase, Estado estado) {
            this.ruta = ruta; this.hashActual = hashActual; this.hashBase = hashBase; this.estado = estado;
        }
    }

    private IntegridadArchivos() {}

    public static File archivoLineaBase() {
        return Config.carpetaDatos().resolve("integridad.sha256").toFile();
    }

    /** Archivos que se vigilan: el .jar en ejecución, BodegaLaPeruanita.jar si está junto a él y la configuración. */
    public static List<File> archivosMonitoreados() {
        List<File> l = new ArrayList<>();
        File jar = Config.jarEnEjecucion();
        if (jar != null) {
            l.add(jar);
            File otro = new File(jar.getParentFile(), "BodegaLaPeruanita.jar");
            if (otro.isFile() && !otro.equals(jar)) l.add(otro);
        } else {
            File persistence = new File(Config.carpetaAplicacion(), "META-INF/persistence.xml");
            if (persistence.isFile()) l.add(persistence);
        }
        File cfg = Config.getArchivoConfiguracion();
        if (cfg != null && cfg.isFile()) l.add(cfg);
        return l;
    }

    public static String sha256(File f) throws IOException {
        try (java.io.InputStream in = Files.newInputStream(f.toPath())) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> leerLineaBase() {
        Map<String, String> m = new LinkedHashMap<>();
        File f = archivoLineaBase();
        if (!f.isFile()) return null;
        try {
            for (String linea : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                int i = linea.indexOf("  ");
                if (i > 0) m.put(linea.substring(i + 2).trim(), linea.substring(0, i).trim());
            }
        } catch (IOException e) {
            Log.error("No se pudo leer la línea base de integridad", e);
            return null;
        }
        return m;
    }

    /** Guarda los hashes actuales como línea base (acción de administrador, queda auditada). */
    public static void generarLineaBase(Usuario u) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (File f : archivosMonitoreados()) {
            sb.append(sha256(f)).append("  ").append(f.getAbsolutePath()).append("\n");
        }
        Path destino = archivoLineaBase().toPath();
        Files.write(destino, sb.toString().getBytes(StandardCharsets.UTF_8));
        AuditoriaDAO.registrar(u, "Integridad", "LINEA_BASE_GENERADA", archivosMonitoreados().size() + " archivo(s)");
    }

    /** Compara los archivos actuales contra la línea base. */
    public static List<Fila> verificar() {
        List<Fila> filas = new ArrayList<>();
        Map<String, String> base = leerLineaBase();
        List<File> actuales = archivosMonitoreados();
        List<String> vistos = new ArrayList<>();
        for (File f : actuales) {
            String ruta = f.getAbsolutePath();
            vistos.add(ruta);
            String h;
            try { h = sha256(f); } catch (IOException e) { h = "(ilegible)"; }
            if (base == null || !base.containsKey(ruta)) filas.add(new Fila(ruta, h, "", Estado.NUEVO));
            else filas.add(new Fila(ruta, h, base.get(ruta),
                    base.get(ruta).equalsIgnoreCase(h) ? Estado.OK : Estado.MODIFICADO));
        }
        if (base != null) {
            for (Map.Entry<String, String> e : base.entrySet()) {
                if (!vistos.contains(e.getKey())) filas.add(new Fila(e.getKey(), "", e.getValue(), Estado.FALTANTE));
            }
        }
        return filas;
    }

    /**
     * Se ejecuta al iniciar el sistema. Si no hay línea base, la crea (primer arranque).
     * Si algún archivo cambió, registra auditoría y deja una alerta para el administrador.
     */
    public static void verificarAlIniciar() {
        try {
            if (!archivoLineaBase().isFile()) {
                generarLineaBase(null);
                return;
            }
            for (Fila f : verificar()) {
                if (f.estado == Estado.MODIFICADO || f.estado == Estado.FALTANTE) {
                    String detalle = f.estado + ": " + new File(f.ruta).getName();
                    AuditoriaDAO.registrar(null, "(sistema)", "Integridad", "ARCHIVO_" + f.estado, detalle);
                    AuditoriaDAO.crearAlerta("INTEGRIDAD", null,
                            "Integridad: el archivo «" + new File(f.ruta).getName() + "» " +
                            (f.estado == Estado.MODIFICADO ? "fue modificado" : "ya no existe")
                            + " desde la última línea base (SHA-256 distinto).");
                }
            }
        } catch (IOException | RuntimeException e) {
            Log.error("Verificación de integridad", e);
        }
    }
}
