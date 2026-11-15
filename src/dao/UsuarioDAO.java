package dao;

import conexion.ConexionBD;
import modelo.ResultadoLogin;
import modelo.Usuario;
import util.Seguridad;
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
                    AuditoriaDAO.registrar(null, usuario, "Autenticación", "LOGIN_FALLIDO",
                            "Usuario inexistente");
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
                u.setPassword(""); // el hash nunca sale de la BD
                u.setRol(rs.getString("rol"));
                u.setEstado(estado);
            }
        }

        // 1) Cuenta bloqueada por intentos fallidos
        if (minBloqueo > 0) {
            AuditoriaDAO.registrar(id, usuario, "Autenticación", "LOGIN_RECHAZADO_CUENTA_BLOQUEADA",
                    "Intento de ingreso con la cuenta bloqueada");
            return new ResultadoLogin(ResultadoLogin.Tipo.BLOQUEADO, null, 0, minBloqueo);
        }

        // 2) Contraseña incorrecta -> sumar intento
        if (passBD == null || !Seguridad.verificar(password, passBD)) {
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
                AuditoriaDAO.registrar(id, usuario, "Autenticación", "CUENTA_BLOQUEADA",
                        MAX_INTENTOS + " intentos fallidos; bloqueo de " + MINUTOS_BLOQUEO + " min");
                AuditoriaDAO.crearAlerta("CUENTA_BLOQUEADA", u.getUsuario(),
                        "La cuenta «" + u.getUsuario() + "» fue bloqueada " + MINUTOS_BLOQUEO
                        + " min por " + MAX_INTENTOS + " intentos fallidos de contraseña.");
                return new ResultadoLogin(ResultadoLogin.Tipo.BLOQUEADO, null, 0, MINUTOS_BLOQUEO);
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE usuarios SET intentos_fallidos = ? WHERE id_usuario = ?")) {
                ps.setInt(1, intentos);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            AuditoriaDAO.registrar(id, usuario, "Autenticación", "LOGIN_FALLIDO",
                    "Contraseña incorrecta (intento " + intentos + " de " + MAX_INTENTOS + ")");
            return new ResultadoLogin(ResultadoLogin.Tipo.INCORRECTO, null,
                    MAX_INTENTOS - intentos, 0);
        }

        // 3) Credenciales correctas pero usuario inactivo
        if (!"Activo".equalsIgnoreCase(estado)) {
            AuditoriaDAO.registrar(id, usuario, "Autenticación", "LOGIN_RECHAZADO_INACTIVO",
                    "Credenciales correctas pero la cuenta está inactiva");
            return new ResultadoLogin(ResultadoLogin.Tipo.INACTIVO, null, 0, 0);
        }

        // 4) Acceso correcto -> reiniciar contador
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = NULL "
                + "WHERE id_usuario = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }

        AuditoriaDAO.registrar(id, u.getUsuario(), "Autenticación", "INICIO_SESION",
                "Rol " + u.getRol());
        return new ResultadoLogin(ResultadoLogin.Tipo.OK, u, 0, 0);

    } catch (SQLException e) {
        util.Log.error(e);
        return new ResultadoLogin(ResultadoLogin.Tipo.ERROR, null, 0, 0);
    }
}

    // LISTAR USUARIOS
public List<Usuario> listarTodos() {

    List<Usuario> lista = new ArrayList<>();

    String sql = "SELECT u.id_usuario, u.nombre_completo, u.usuario, "
            + "r.nombre AS rol, u.estado::text AS estado "
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
            u.setPassword(""); // datos sensibles: la contraseña/hash nunca se carga en pantalla
            u.setRol(rs.getString("rol"));
            u.setEstado(rs.getString("estado"));

            lista.add(u);
        }

    } catch (SQLException e) {
        util.Log.error(e);
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
        ps.setString(3, hashSiHaceFalta(u.getPassword()));

        ps.setString(4, u.getRol());

        ps.setString(5, u.getEstado());

        ps.setString(6, u.getRol());

        return ps.executeUpdate() > 0;

    } catch (SQLException e) {
        util.Log.error(e);
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
        ps.setString(3, hashSiHaceFalta(u.getPassword()));
        ps.setString(4, u.getRol());
        ps.setString(5, u.getEstado());
        ps.setString(6, u.getRol());
        ps.setInt(7, u.getIdUsuario());

        return ps.executeUpdate() > 0;

    } catch (SQLException e) {
        util.Log.error(e);
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
        util.Log.error(e);
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
        util.Log.error(e);
    }

    return false;
}

    // =====================================================================
    //  Control de cuentas (bloqueo de usuarios) y utilidades de seguridad
    // =====================================================================

    public enum ResultadoCambio { OK, PROPIA_CUENTA, ULTIMO_ADMIN, NO_EXISTE, ERROR }

    /** Guarda siempre un hash: si ya viene cifrada se deja igual, si es texto claro se cifra. */
    private static String hashSiHaceFalta(String password) {
        return Seguridad.esHash(password) ? password : Seguridad.hashear(password);
    }

    /** Devuelve {rol, estado} ACTUALES del usuario leídos de la BD (null si no existe o hay error). */
    public String[] obtenerRolYEstado(int idUsuario) {
        String sql = "SELECT rol::text, estado::text FROM usuarios WHERE id_usuario = ?";
        try (Connection con = ConexionBD.getConexion()) {
            if (con == null) return null;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return new String[] { rs.getString(1), rs.getString(2) };
                }
            }
        } catch (SQLException e) {
            util.Log.error("No se pudo leer el rol/estado del usuario", e);
        }
        return null;
    }

    private int contarAdministradoresActivos(Connection con, int idExcluido) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE rol = 'Administrador'::rol_usuario "
                + "AND estado = 'Activo'::estado_general AND id_usuario <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Reglas del bloqueo de usuarios: el administrador no puede desactivar (ni quitarse el rol de)
     * su propia cuenta, y siempre debe quedar al menos un Administrador activo.
     * @param nuevoRol null si el rol no cambia
     */
    public ResultadoCambio validarCambioUsuario(int idObjetivo, String nuevoRol, String nuevoEstado, int idActor) {
        try (Connection con = ConexionBD.getConexion()) {
            if (con == null) return ResultadoCambio.ERROR;
            String rolActual, estadoActual;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT rol::text, estado::text FROM usuarios WHERE id_usuario = ?")) {
                ps.setInt(1, idObjetivo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return ResultadoCambio.NO_EXISTE;
                    rolActual = rs.getString(1);
                    estadoActual = rs.getString(2);
                }
            }
            boolean quedaraInactivo = nuevoEstado != null && !"Activo".equalsIgnoreCase(nuevoEstado);
            boolean dejaraDeSerAdmin = nuevoRol != null && !"Administrador".equalsIgnoreCase(nuevoRol);

            if (idObjetivo == idActor && (quedaraInactivo || dejaraDeSerAdmin)) {
                return ResultadoCambio.PROPIA_CUENTA;
            }
            boolean eraAdminActivo = "Administrador".equalsIgnoreCase(rolActual)
                    && "Activo".equalsIgnoreCase(estadoActual);
            if (eraAdminActivo && (quedaraInactivo || dejaraDeSerAdmin)
                    && contarAdministradoresActivos(con, idObjetivo) == 0) {
                return ResultadoCambio.ULTIMO_ADMIN;
            }
            return ResultadoCambio.OK;
        } catch (SQLException e) {
            util.Log.error("No se pudo validar el cambio de usuario", e);
            return ResultadoCambio.ERROR;
        }
    }

    /** Activa/desactiva una cuenta aplicando las reglas anteriores. */
    public ResultadoCambio cambiarEstado(int idUsuario, String estado, int idActor) {
        ResultadoCambio r = validarCambioUsuario(idUsuario, null, estado, idActor);
        if (r != ResultadoCambio.OK) return r;
        return cambiarEstado(idUsuario, estado) ? ResultadoCambio.OK : ResultadoCambio.ERROR;
    }

    /** true si ya existe otro usuario (distinto del indicado) con ese nombre de acceso. */
    public boolean existeOtroUsuario(String usuario, int idExcluido) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE LOWER(usuario) = LOWER(?) AND id_usuario <> ?";
        try (Connection con = ConexionBD.getConexion()) {
            if (con == null) return true;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, usuario);
                ps.setInt(2, idExcluido);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            util.Log.error("No se pudo verificar el usuario", e);
            return true;
        }
    }
}
