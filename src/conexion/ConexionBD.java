package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import util.Config;

public class ConexionBD {

    private static final String URL_BASE = Config.get(
            "BODEGA_DB_URL", "db.url", "jdbc:postgresql://localhost:5432/BODEGA_LAPERUANITA");
    // La instalación local de PostgreSQL no tiene SSL habilitado. Las instalaciones
    // que sí usan TLS pueden sobrescribir este valor con BODEGA_DB_SSLMODE o db.sslmode.
    private static final String SSLMODE = Config.get("BODEGA_DB_SSLMODE", "db.sslmode", "disable");

    private static final String ADMIN_USER = Config.get("BODEGA_DB_ADMIN_USER", "db.admin.user", "postgres");
    private static final String ADMIN_PASS = Config.get("BODEGA_DB_ADMIN_PASSWORD", "db.admin.password", null);

    private static final String APP_USER = Config.get("BODEGA_DB_USER", "db.app.user", null);
    private static final String APP_PASS = Config.get("BODEGA_DB_PASSWORD", "db.app.password", null);

    public static String getUrl() {
        if (URL_BASE.toLowerCase().contains("sslmode=") || URL_BASE.toLowerCase().contains("ssl=")) return URL_BASE;
        return URL_BASE + (URL_BASE.contains("?") ? "&" : "?") + "sslmode=" + SSLMODE;
    }

    /** URL a la base de mantenimiento para preparar la base del POS si todavía no existe. */
    private static String getUrlMantenimiento() {
        int query = URL_BASE.indexOf('?');
        String urlSinParametros = query < 0 ? URL_BASE : URL_BASE.substring(0, query);
        String parametros = query < 0 ? "" : URL_BASE.substring(query);
        int ultimaBarra = urlSinParametros.lastIndexOf('/');
        if (ultimaBarra < "jdbc:postgresql://".length()) {
            throw new IllegalStateException("La URL de PostgreSQL no contiene un nombre de base de datos válido.");
        }
        String url = urlSinParametros.substring(0, ultimaBarra + 1) + "postgres" + parametros;
        if (!parametros.toLowerCase(java.util.Locale.ROOT).contains("sslmode=")
                && !parametros.toLowerCase(java.util.Locale.ROOT).contains("ssl=")) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=" + SSLMODE;
        }
        return url;
    }

    /** Crea la base solo si falta. Requiere bootstrap y una cuenta PostgreSQL con CREATEDB. */
    public static void asegurarBaseDatos() {
        String nombre = getNombreBD();
        if (!nombre.matches("[A-Za-z0-9_]{1,63}")) {
            throw new IllegalStateException("El nombre de la base debe tener de 1 a 63 letras, números o guiones bajos.");
        }
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conexion = DriverManager.getConnection(getUrlMantenimiento(), ADMIN_USER, ADMIN_PASS)) {
                boolean existe;
                try (PreparedStatement consulta = conexion.prepareStatement(
                        "SELECT 1 FROM pg_database WHERE datname = ?")) {
                    consulta.setString(1, nombre);
                    try (ResultSet rs = consulta.executeQuery()) { existe = rs.next(); }
                }
                if (existe) return;
                try (Statement sentencia = conexion.createStatement()) {
                    sentencia.execute("CREATE DATABASE \"" + nombre + "\"");
                }
                util.Log.info("Base de datos creada automáticamente: " + nombre);
            }
        } catch (SQLException e) {
            if ("42P04".equals(e.getSQLState())) return; // Otra instancia la creó al mismo tiempo.
            util.Log.error("No se pudo crear o comprobar la base de datos " + nombre, e);
            throw new IllegalStateException("No se pudo comprobar o crear la base de datos. Verifique PostgreSQL, BODEGA_DB_ADMIN_USER/BODEGA_DB_ADMIN_PASSWORD y el permiso CREATEDB de esa cuenta.", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se encontró el controlador de PostgreSQL.", e);
        }
    }

    public static String getSslMode() { return SSLMODE; }

    public static String getNombreBD() {
        String s = URL_BASE;
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        return s.substring(s.lastIndexOf('/') + 1);
    }

    public static String getHost() {
        String s = URL_BASE.replaceFirst("^jdbc:postgresql://", "");
        s = s.substring(0, s.indexOf('/') < 0 ? s.length() : s.indexOf('/'));
        return s.contains(":") ? s.substring(0, s.indexOf(':')) : s;
    }

    public static String getPuerto() {
        String s = URL_BASE.replaceFirst("^jdbc:postgresql://", "");
        s = s.substring(0, s.indexOf('/') < 0 ? s.length() : s.indexOf('/'));
        return s.contains(":") ? s.substring(s.indexOf(':') + 1) : "5432";
    }

    public static boolean hayCuentaAplicacion() {
        return APP_USER != null && APP_PASS != null && !APP_USER.equalsIgnoreCase(ADMIN_USER);
    }

    public static String getUsuarioAdmin() { return ADMIN_USER; }
    public static String getPasswordAdmin() { return ADMIN_PASS; }
    public static String getUsuarioApp() { return APP_USER; }
    public static String getPasswordApp() { return APP_PASS; }
    public static String getUsuario() { return hayCuentaAplicacion() ? APP_USER : ADMIN_USER; }
    public static String getPassword() { return hayCuentaAplicacion() ? APP_PASS : ADMIN_PASS; }

    private static Connection abrir(String user, String pass) {
        Connection con = null;
        try {
            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection(getUrl(), user, pass);
        } catch (ClassNotFoundException e) {
            util.Log.error("Driver de PostgreSQL no encontrado", e);
        } catch (SQLException e) {
            util.Log.error("No se pudo conectar con la base de datos", e);
        }
        return con;
    }

    public static Connection getConexion() {
        return abrir(getUsuario(), getPassword());
    }
    public static Connection getConexionAdmin() {
        return abrir(ADMIN_USER, ADMIN_PASS);
    }

    public static void main(String[] args) {
        System.out.println("Probando conexion...");
        if (getConexion() != null) {
            System.out.println("¡CONEXION EXITOSA CON POSTGRESQL!");
        } else {
            System.out.println("ERROR: No se pudo conectar.");
        }
    }
}
