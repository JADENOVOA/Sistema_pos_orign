package util;

public class ValidadorPassword {

    public static final int LONGITUD_MINIMA = 12;

    public static final String REQUISITOS =
            "La contraseña debe cumplir con lo siguiente:\n"
            + "  • Mínimo " + LONGITUD_MINIMA + " caracteres\n"
            + "  • Al menos una letra mayúscula (A-Z)\n"
            + "  • Al menos una letra minúscula (a-z)\n"
            + "  • Al menos un número (0-9)\n"
            + "  • Al menos un símbolo (@ # $ % ! ? * etc.)\n"
            + "  • Sin espacios";

    /** Devuelve true si la contraseña cumple TODOS los requisitos. */
    public static boolean esSegura(String p) {
        if (p == null || p.length() < LONGITUD_MINIMA) return false;
        boolean may = false, min = false, num = false, sim = false;
        for (char c : p.toCharArray()) {
            if (Character.isWhitespace(c)) return false;
            if (Character.isUpperCase(c)) may = true;
            else if (Character.isLowerCase(c)) min = true;
            else if (Character.isDigit(c)) num = true;
            else sim = true;
        }
        return may && min && num && sim;
    }
}
