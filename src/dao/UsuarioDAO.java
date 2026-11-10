package dao;

import conexion.ConexionBD;
import modelo.ResultadoLogin;
import modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

public static final int MAX_INTENTOS = 3;
public static final int MINUTOS_BLOQUEO = 15;

public ResultadoLogin autenticar(String usuario, String password) {

    String sql = "SELECT id_usuario, nombre_completo, usuario, password, "
            + "rol::text AS rol, estado::text AS estado, intentos_fallidos, "
            + "CASE WHEN bloqueado_hasta IS NOT NULL AND bloqueado_hasta > now() "
            + "THEN CEIL(EXTRACT(EPOCH FROM (bloqueado_hasta - now())) / 60) "
            + "ELSE 0 END AS min_bloqueo "
            + "FROM usuarios WHERE LOWER(usuario) = LOWER(?)";

    try (Connection con = ConexionBD.getConexion()) {

        if (con == null) {
            return new ResultadoLogin(ResultadoLogin.Tipo.ERROR, null, 0, 0);
        }

        int id, intentos, minBloqueo;
        String passBD, estado;
        Usuario u = new Usuario();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // usuario inexistente: mensaje genérico
                    return new ResultadoLogin(ResultadoLogin.Tipo.INCORRECTO, null, 0, 0);
                }
                id = rs.getInt("id_usuario");
                intentos = rs.getInt("intentos_fallidos");
                minBloqueo = rs.getInt("min_bloqueo");
                passBD = rs.getString("password");
                estado = rs.getString("estado");

                u.setIdUsuario(id);
                u.setNombreCompleto(rs.getString("nombre_completo"));
                u.setUsuario(rs.getString("usuario"));
                u.setPassword(passBD);
                u.setRol(rs.getString("rol"));
                u.setEstado(estado);
            }
        }

        // 1) Cuenta bloqueada por intentos fallidos
        if (minBloqueo > 0) {
            return new ResultadoLogin(ResultadoLogin.Tipo.BLOQUEADO, null, 0, minBloqueo);
        }

        // 2) Contraseña incorrecta -> sumar intento
        if (passBD == null || !passBD.equals(password)) {
            intentos++;
            if (intentos >= MAX_INTENTOS) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE usuarios SET intentos_fallidos = 0, "
                        + "bloqueado_hasta = now() + (? * interval '1 minute') "
                        + "WHERE id_usuario = ?")) {
                    ps.setInt(1, MINUTOS_BLOQUEO);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
                return new ResultadoLogin(ResultadoLogin.Tipo.BLOQUEADO, null, 0, MINUTOS_BLOQUEO);
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE usuarios SET intentos_fallidos = ? WHERE id_usuario = ?")) {
                ps.setInt(1, intentos);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            return new ResultadoLogin(ResultadoLogin.Tipo.INCORRECTO, null,
                    MAX_INTENTOS - intentos, 0);
        }

        // 3) Credenciales correctas pero usuario inactivo
        if (!"Activo".equalsIgnoreCase(estado)) {
            return new ResultadoLogin(ResultadoLogin.Tipo.INACTIVO, null, 0, 0);
        }

        // 4) Acceso correcto -> reiniciar contador
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = NULL "
                + "WHERE id_usuario = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }

        return new ResultadoLogin(ResultadoLogin.Tipo.OK, u, 0, 0);

    } catch (SQLException e) {
        e.printStackTrace();
        return new ResultadoLogin(ResultadoLogin.Tipo.ERROR, null, 0, 0);
    }
}

    // LISTAR USUARIOS
public List<Usuario> listarTodos() {

    List<Usuario> lista = new ArrayList<>();

    String sql = "SELECT u.id_usuario, u.nombre_completo, u.usuario, "
            + "u.password, r.nombre AS rol, u.estado::text AS estado "
            + "FROM usuarios u "
            + "INNER JOIN roles r ON u.id_rol = r.id_rol "
            + "ORDER BY u.id_usuario";

    try (
        Connection con = ConexionBD.getConexion();
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()
    ) {

        while (rs.next()) {

            Usuario u = new Usuario();

            u.setIdUsuario(rs.getInt("id_usuario"));
            u.setNombreCompleto(rs.getString("nombre_completo"));
            u.setUsuario(rs.getString("usuario"));
            u.setPassword(rs.getString("password"));
            u.setRol(rs.getString("rol"));
            u.setEstado(rs.getString("estado"));

            lista.add(u);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return lista;
}
public boolean insertar(Usuario u) {

    String sql = "INSERT INTO usuarios "
            + "(nombre_completo, usuario, password, rol, estado, id_rol) "
            + "VALUES (?, ?, ?, ?::rol_usuario, ?::estado_general, "
            + "(SELECT id_rol FROM roles WHERE LOWER(nombre) = LOWER(?)))";

    try (
        Connection con = ConexionBD.getConexion();
        PreparedStatement ps = con.prepareStatement(sql)
    ) {

        ps.setString(1, u.getNombreCompleto());
        ps.setString(2, u.getUsuario());
        ps.setString(3, u.getPassword());

        ps.setString(4, u.getRol());

        ps.setString(5, u.getEstado());

        ps.setString(6, u.getRol());

        return ps.executeUpdate() > 0;

    } catch (SQLException e) {
        e.printStackTrace();
        System.err.println("Error al insertar usuario: " + e.getMessage());
        return false;
    }
}
public boolean actualizar(Usuario u) {

    String sql = "UPDATE usuarios SET "
            + "nombre_completo = ?, "
            + "usuario = ?, "
            + "password = ?, "
            + "rol = ?::rol_usuario, "
            + "estado = ?::estado_general, "
            + "id_rol = (SELECT id_rol FROM roles WHERE LOWER(nombre) = LOWER(?)) "
            + "WHERE id_usuario = ?";

    try (
        Connection con = ConexionBD.getConexion();
        PreparedStatement ps = con.prepareStatement(sql)
    ) {

        ps.setString(1, u.getNombreCompleto());
        ps.setString(2, u.getUsuario());
        ps.setString(3, u.getPassword());
        ps.setString(4, u.getRol());
        ps.setString(5, u.getEstado());
        ps.setString(6, u.getRol());
        ps.setInt(7, u.getIdUsuario());

        return ps.executeUpdate() > 0;

    } catch (SQLException e) {
        e.printStackTrace();
        System.err.println("Error al actualizar usuario: " + e.getMessage());
        return false;
    }
}
public boolean cambiarEstado(int idUsuario, String estado) {

    String sql = "UPDATE usuarios "
            + "SET estado = ?::estado_general "
            + "WHERE id_usuario = ?";

    try (
        Connection con = ConexionBD.getConexion();
        PreparedStatement ps = con.prepareStatement(sql)
    ) {

        ps.setString(1, estado);
        ps.setInt(2, idUsuario);

        return ps.executeUpdate() > 0;

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

public boolean existeUsuario(String usuario) {

    String sql = "SELECT COUNT(*) "
            + "FROM usuarios "
            + "WHERE LOWER(usuario) = LOWER(?)";

    try (
        Connection con = ConexionBD.getConexion();
        PreparedStatement ps = con.prepareStatement(sql)
    ) {

        ps.setString(1, usuario);

        try (ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return false;
}
}