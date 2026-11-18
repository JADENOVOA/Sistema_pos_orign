package util;

import com.sun.jna.platform.win32.Crypt32Util;
import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Bóveda local de secretos cifrada con DPAPI y ligada al perfil de Windows. */
public final class BovedaSecretos {
    private static final Path ARCHIVO = Config.carpetaDatos().resolve("secretos.bin");
    private BovedaSecretos() {}

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    private static Properties leerTodas() throws Exception {
        Properties p = new Properties();
        if (Files.isRegularFile(ARCHIVO)) {
            p.load(new java.io.ByteArrayInputStream(
                    Crypt32Util.cryptUnprotectData(Files.readAllBytes(ARCHIVO))));
        }
        return p;
    }

    private static void guardarTodas(Properties p) throws Exception {
        if (!windows()) throw new IllegalStateException("La bóveda requiere Windows y DPAPI.");
        java.io.ByteArrayOutputStream claro = new java.io.ByteArrayOutputStream();
        p.store(claro, "Secretos Bodega La Peruanita POS; cifrado por Windows DPAPI");
        byte[] protegido = Crypt32Util.cryptProtectData(claro.toByteArray());
        Files.createDirectories(ARCHIVO.getParent());
        Path temporal = ARCHIVO.resolveSibling(ARCHIVO.getFileName() + ".tmp");
        Files.write(temporal, protegido);
        try { Files.move(temporal, ARCHIVO, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (IOException e) { Files.move(temporal, ARCHIVO, StandardCopyOption.REPLACE_EXISTING); }
    }

    public static String obtener(String nombre) {
        if (!windows()) return null;
        try { return leerTodas().getProperty(nombre); }
        catch (Exception e) { Log.error("No se pudo leer la bóveda protegida", e); return null; }
    }

    public static void guardar(String nombre, String valor) throws Exception {
        if (nombre == null || !nombre.matches("BODEGA_[A-Z0-9_]+") || valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("Nombre o valor de secreto no válido.");
        }
        Properties p = leerTodas(); p.setProperty(nombre, valor); guardarTodas(p);
    }

    /** Migra claves de contraseña legacy desde bodega.properties y las elimina del archivo tras cifrarlas. */
    static synchronized void migrarConfiguracionLegacy(Properties propiedades, Path archivo) throws Exception {
        if (archivo == null || !Files.isRegularFile(archivo)) return;
        String[][] claves = {
            {"db.admin.password", "BODEGA_DB_ADMIN_PASSWORD"},
            {"db.app.password", "BODEGA_DB_PASSWORD"},
            {"mail.smtp.password", "BODEGA_SMTP_PASSWORD"}
        };
        Properties secretos = leerTodas();
        boolean hayLegacy = false;
        for (String[] clave : claves) {
            String valor = propiedades.getProperty(clave[0]);
            if (valor != null && !valor.isBlank()) {
                secretos.setProperty(clave[1], valor);
                hayLegacy = true;
            }
        }
        if (!hayLegacy) return;
        guardarTodas(secretos);
        java.util.List<String> lineas = Files.readAllLines(archivo, StandardCharsets.ISO_8859_1);
        java.util.List<String> seguras = new java.util.ArrayList<>();
        for (String linea : lineas) {
            String t = linea.trim();
            if (t.startsWith("#") || t.startsWith("!")) { seguras.add(linea); continue; }
            if (t.matches("(?i)^(db\\.(admin|app)\\.password|mail\\.smtp\\.password)(?:\\s*[=:].*|\\s+.*)?$")) continue;
            seguras.add(linea);
        }
        Files.write(archivo, seguras, StandardCharsets.ISO_8859_1);
        for (String[] clave : claves) propiedades.remove(clave[0]);
        Log.info("Credenciales heredadas migradas a la bóveda cifrada de Windows.");
    }

    /** Provisiona secretos sin mostrarlos ni aceptarlos como argumento de línea de comandos. */
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--migrate".equals(args[0])) {
            Config.migrarSecretosLegados();
            System.out.println("La configuración fue revisada; las claves heredadas se cifraron si estaban presentes.");
            return;
        }
        Console consola = System.console();
        if (consola == null || args.length != 1) {
            System.err.println("Uso: java util.BovedaSecretos NOMBRE_VARIABLE (ej.: BODEGA_DB_PASSWORD)");
            System.exit(2);
        }
        char[] uno = consola.readPassword("Ingrese el secreto para %s: ", args[0]);
        char[] dos = consola.readPassword("Repita el secreto: ");
        if (!java.util.Arrays.equals(uno, dos)) throw new IllegalArgumentException("Los valores no coinciden.");
        guardar(args[0], new String(uno));
        java.util.Arrays.fill(uno, '\0'); java.util.Arrays.fill(dos, '\0');
        System.out.println("Secreto guardado cifrado para este usuario de Windows.");
    }
}
