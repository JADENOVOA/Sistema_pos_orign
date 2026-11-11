package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {
    // Parámetros de conexión a PostgreSQL
    private static final String URL = System.getenv().getOrDefault(
            "BODEGA_DB_URL", "jdbc:postgresql://localhost:5432/BODEGA_LAPERUANITA");
    private static final String USER = System.getenv().getOrDefault("BODEGA_DB_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("BODEGA_DB_PASSWORD", "root");

    public static String getUrl() { return URL; }
    public static String getUsuario() { return USER; }
    public static String getPassword() { return PASSWORD; }

    public static Connection getConexion() {
        Connection con = null;
        try {
            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection(getUrl(), getUsuario(), getPassword());
        } catch (ClassNotFoundException e) {
            System.err.println("Driver de PostgreSQL no encontrado: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Error al conectar con la base de datos: " + e.getMessage());
        }
        return con;
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
