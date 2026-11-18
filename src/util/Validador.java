package util;

import java.util.regex.Pattern;

/**
 * Validación de datos de entrada. Rechaza vacíos, negativos, texto en campos numéricos,
 * notación científica ("1e9"), valores enormes, textos muy largos y caracteres de script (< > etc.).
 * Todos los métodos devuelven null si el dato es válido, o el mensaje de error a mostrar.
 */
public final class Validador {

    public static final int MAX_NOMBRE = 100;
    public static final int MAX_TEXTO = 150;
    public static final int MAX_USUARIO = 30;
    public static final int MAX_PASSWORD = 64;
    public static final double MAX_DINERO = 99_999.99;
    public static final int MAX_CANTIDAD = 99_999;

    private static final Pattern DINERO = Pattern.compile("^\\d{1,5}([.,]\\d{1,2})?$");
    private static final Pattern ENTERO = Pattern.compile("^\\d{1,5}$");
    private static final Pattern USUARIO = Pattern.compile("^[A-Za-z0-9._-]{3," + MAX_USUARIO + "}$");
    /** Caracteres peligrosos para HTML / scripts / SQL: < > " ` \ ; { } | ^ ~ $ = */
    private static final Pattern PELIGROSOS = Pattern.compile("[<>\"`\\\\;{}|^~$=]");
    private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");

    private Validador() {}

    /** Texto libre: obligatorio (o no), longitud máxima y sin caracteres de script. */
    public static String texto(String valor, String campo, int maximo, boolean obligatorio) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) return obligatorio ? "El campo «" + campo + "» es obligatorio." : null;
        if (v.length() > maximo) return "El campo «" + campo + "» no puede superar " + maximo + " caracteres.";
        if (PELIGROSOS.matcher(v).find() || CONTROL.matcher(v).find()) {
            return "El campo «" + campo + "» contiene caracteres no permitidos ( < > \" ; = \\ { } | ).";
        }
        return null;
    }

    public static String usuario(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) return "El campo «Usuario» es obligatorio.";
        if (!USUARIO.matcher(v).matches()) {
            return "El usuario debe tener de 3 a " + MAX_USUARIO
                    + " caracteres: solo letras, números, punto, guion y guion bajo.";
        }
        return null;
    }

    public static String password(String valor) {
        if (valor != null && valor.length() > MAX_PASSWORD) {
            return "La contraseña no puede superar " + MAX_PASSWORD + " caracteres.";
        }
        return null;
    }

    /** Correo opcional; si se informa, debe tener una forma válida y longitud RFC habitual. */
    public static String email(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String v = valor.trim();
        if (v.length() > 254 || !v.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$")) {
            return "Ingrese un correo electrónico válido (máximo 254 caracteres).";
        }
        return null;
    }

    /** Monto en soles: 0 a 99,999.99, máximo 2 decimales, sin signo ni notación científica. */
    public static String dinero(String valor, String campo, boolean permitirCero) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) return "El campo «" + campo + "» es obligatorio.";
        if (!DINERO.matcher(v).matches()) {
            return "«" + campo + "» debe ser un monto válido (solo números, máximo 2 decimales, sin signos).";
        }
        double d = aDouble(v);
        if (d > MAX_DINERO) return "«" + campo + "» no puede superar S/. " + String.format("%,.2f", MAX_DINERO) + ".";
        if (!permitirCero && d <= 0) return "«" + campo + "» debe ser mayor a 0.";
        return null;
    }

    /** Entero 0 a 99,999. */
    public static String entero(String valor, String campo, boolean permitirCero) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) return "El campo «" + campo + "» es obligatorio.";
        if (!ENTERO.matcher(v).matches()) {
            return "«" + campo + "» debe ser un número entero positivo (sin letras, signos ni decimales).";
        }
        int n = Integer.parseInt(v);
        if (!permitirCero && n <= 0) return "«" + campo + "» debe ser mayor a 0.";
        return null;
    }

    public static double aDouble(String v) { return Double.parseDouble(v.trim().replace(",", ".")); }

    /** Escapa HTML para mostrar texto de usuario dentro de un JLabel con "<html>". */
    public static String escaparHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
