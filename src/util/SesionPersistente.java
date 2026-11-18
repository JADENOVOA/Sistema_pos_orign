package util;

import com.sun.jna.platform.win32.Crypt32Util;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Guarda únicamente un token aleatorio cifrado por Windows DPAPI para el usuario actual. */
public final class SesionPersistente {
    private static final Path ARCHIVO = Config.carpetaDatos().resolve("sesion-recordada.bin");
    private SesionPersistente() {}

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    public static void guardar(String token) throws Exception {
        if (!windows()) throw new IllegalStateException("Recordarme requiere Windows y DPAPI.");
        byte[] protegido = Crypt32Util.cryptProtectData(token.getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(ARCHIVO.getParent());
        Files.write(ARCHIVO, protegido);
    }

    public static String leer() {
        try {
            if (!windows() || !Files.isRegularFile(ARCHIVO)) return null;
            byte[] claro = Crypt32Util.cryptUnprotectData(Files.readAllBytes(ARCHIVO));
            return new String(claro, StandardCharsets.UTF_8);
        } catch (Exception e) { eliminar(); return null; }
    }

    public static void eliminar() {
        try { Files.deleteIfExists(ARCHIVO); }
        catch (Exception e) { Log.error("Eliminar token de sesión local", e); }
    }
}
