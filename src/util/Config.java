package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Lee la configuración del sistema SIN dejar claves escritas en el código.
 * Orden de prioridad: variable de entorno -> archivo bodega.properties -> valor por defecto.
 * El archivo se busca en: carpeta de trabajo, carpeta del .jar y carpeta ~/.bodega_laperuanita.
 */
public final class Config {

    private static final Properties ARCHIVO = new Properties();
    private static File archivoCargado;

    static {
        for (File f : candidatos()) {
            if (f.isFile()) {
                try (InputStream in = new FileInputStream(f)) {
                    ARCHIVO.load(in);
                    archivoCargado = f;
                    break;
                } catch (Exception e) {
                    System.err.println("No se pudo leer la configuración: " + f);
                }
            }
        }
    }

    private Config() {}

    private static File[] candidatos() {
        File carpetaJar = carpetaAplicacion();
        return new File[] {
            new File("bodega.properties"),
            new File(carpetaJar, "bodega.properties"),
            new File(carpetaJar, ".." + File.separator + "bodega.properties"),
            new File(System.getProperty("user.home"), ".bodega_laperuanita" + File.separator + "bodega.properties")
        };
    }

    /** Carpeta donde está el .jar (o las clases compiladas). */
    public static File carpetaAplicacion() {
        try {
            File f = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile() ? f.getParentFile() : f;
        } catch (URISyntaxException | RuntimeException e) {
            return new File(".");
        }
    }

    /** .jar en ejecución, o null si se ejecuta desde clases compiladas (IDE / Maven exec). */
    public static File jarEnEjecucion() {
        try {
            File f = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile() ? f : null;
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }

    public static File getArchivoConfiguracion() { return archivoCargado; }

    /** @param claveEnv variable de entorno; @param clave clave en el archivo. */
    public static String get(String claveEnv, String clave, String porDefecto) {
        String v = claveEnv == null ? null : System.getenv(claveEnv);
        if ((v == null || v.isBlank()) && claveEnv != null && claveEnv.endsWith("_PASSWORD")) {
            v = BovedaSecretos.obtener(claveEnv);
        }
        if (v == null || v.isBlank()) v = ARCHIVO.getProperty(clave);
        return (v == null || v.isBlank()) ? porDefecto : v.trim();
    }

    /** Cifra secretos legacy antes de inicializar la conexión y limpia el archivo externo. */
    public static void migrarSecretosLegados() throws Exception {
        BovedaSecretos.migrarConfiguracionLegacy(ARCHIVO, archivoCargado == null ? null : archivoCargado.toPath());
    }

    /** Carpeta de datos del sistema (respaldos, logs, línea base de integridad). */
    public static Path carpetaDatos() {
        String dir = get("BODEGA_DATA_DIR", "data.dir", null);
        File base = dir != null ? new File(dir)
                : new File(System.getProperty("user.home"), ".bodega_laperuanita");
        base.mkdirs();
        return base.toPath();
    }
}
