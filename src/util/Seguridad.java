package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Cifrado de contraseñas: PBKDF2-HMAC-SHA256 con sal aleatoria única por usuario. */
public final class Seguridad {

    private static final String PREFIJO = "pbkdf2$";
    private static final int ITERACIONES = 120_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Seguridad() {}

    /** Devuelve "pbkdf2$iteraciones$sal$hash" (todo en Base64). */
    public static String hashear(String password) {
        byte[] sal = new byte[BYTES_SAL];
        RANDOM.nextBytes(sal);
        byte[] hash = derivar(password.toCharArray(), sal, ITERACIONES);
        return PREFIJO + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean esHash(String almacenado) {
        return almacenado != null && almacenado.startsWith(PREFIJO) && almacenado.split("\\$").length == 4;
    }

    /** Compara la contraseña escrita con el hash guardado (tiempo constante). */
    public static boolean verificar(String password, String almacenado) {
        if (password == null || !esHash(almacenado)) return false;
        try {
            String[] p = almacenado.split("\\$");
            int iter = Integer.parseInt(p[1]);
            byte[] sal = Base64.getDecoder().decode(p[2]);
            byte[] esperado = Base64.getDecoder().decode(p[3]);
            byte[] calculado = derivar(password.toCharArray(), sal, iter);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] derivar(char[] password, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, BITS_HASH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Algoritmo de cifrado no disponible", e);
        }
    }

    /** SHA-256 de un texto, en hexadecimal. */
    public static String sha256(byte[] datos) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(datos);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256(String texto) { return sha256(texto.getBytes(StandardCharsets.UTF_8)); }
}
