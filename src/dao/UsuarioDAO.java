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
import java.security.SecureRandom;
import java.util.Base64;

public class UsuarioDAO {

public static final int MAX_INTENTOS = 3;
public static final int MINUTOS_BLOQUEO = 15;
private static final SecureRandom RANDOM = new SecureRandom();
public static final int DIAS_SESION_RECORDADA = 14;

public String crearSesionRecordada(int idUsuario) {
    byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    String sql = "INSERT INTO sesiones_recordadas (id_usuario, token_hash, expira_en) VALUES (?, ?, now() + interval '14 days')";
    try (Connection con = ConexionBD.getConexion()) {
        if (con == null) return null;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario); ps.setString(2, hashToken(token));
            return ps.executeUpdate() == 1 ? token : null;
        }
    } catch (SQLException | RuntimeException e) { util.Log.error("Crear sesión recordada", e); return null; }
}

public Usuario autenticarSesionRecordada(String token) {
    if (token == null || token.length() < 40 || token.length() > 64) return null;
    String sql = "SELECT u.id_usuario,u.nombre_completo,u.usuario,u.rol::text AS rol,u.estado::text AS estado "
            + "FROM sesiones_recordadas s JOIN usuarios u ON u.id_usuario=s.id_usuario "
            + "WHERE s.token_hash=? AND s.revocado_en IS NULL AND s.expira_en>now() AND u.estado::text='Activo'";
    try (Connection con = ConexionBD.getConexion()) {
        if (con == null) return null;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hashToken(token));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Usuario u = new Usuario(); u.setIdUsuario(rs.getInt("id_usuario"));
                u.setNombreCompleto(rs.getString("nombre_completo")); u.setUsuario(rs.getString("usuario"));
                u.setPassword(""); u.setRol(rs.getString("rol")); u.setEstado(rs.getString("estado"));
                return u;
            }
        }
    } catch (SQLException e) { util.Log.error("Validar sesión recordada", e); return null; }
}

public void revocarSesionRecordada(String token) {
    if (token == null || token.length() < 40 || token.length() > 64) return;
    try (Connection con = ConexionBD.getConexion()) {
        if (con == null) return;
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE sesiones_recordadas SET revocado_en=now() WHERE token_hash=? AND revocado_en IS NULL")) {
            ps.setString(1, hashToken(token)); ps.executeUpdate();
        }
    } catch (SQLException e) { util.Log.error("Revocar sesión recordada", e); }
}

public enum ResultadoRecuperacion { SOLICITUD_ACEPTADA, CORREO_NO_CONFIGURADO, ERROR }

/** Inicia una recuperación sin revelar si existe una cuenta; SMTP debe estar configurado para enviar el código. */
public ResultadoRecuperacion solicitarRecuperacion(String email) {
    String correo = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    if (!correo.matches("^[^\\s@]{1,64}@[^\\s@]{1,190}\\.[^\\s@]{2,}$")) return ResultadoRecuperacion.SOLICITUD_ACEPTADA;
    byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
    String codigo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    String hash = hashToken(codigo);
    try (Connection con = ConexionBD.getConexion()) {
        if (con == null) return ResultadoRecuperacion.ERROR;
        con.setAutoCommit(false);
        Integer id = null;
        String nombre = null;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_usuario, nombre_completo FROM usuarios WHERE lower(email)=? AND estado::text='Activo' FOR UPDATE")) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) { id = rs.getInt(1); nombre = rs.getString(2); } }
        }
        if (id == null) return ResultadoRecuperacion.SOLICITUD_ACEPTADA;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT count(*) FROM recuperacion_password WHERE id_usuario=? AND creado_en > now() - interval '24 hours'")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) >= 5) return ResultadoRecuperacion.SOLICITUD_ACEPTADA;
            }
        }
        // Rechaza reenvíos frecuentes por cuenta; no se crea un código ni se envía otro correo en 60 segundos.
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM recuperacion_password WHERE id_usuario=? AND creado_en > now() - interval '60 seconds'")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return ResultadoRecuperacion.SOLICITUD_ACEPTADA; }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE recuperacion_password SET usado_en=now() WHERE id_usuario=? AND usado_en IS NULL")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO recuperacion_password (id_usuario, token_hash, expira_en) VALUES (?, ?, now() + interval '15 minutes')")) {
            ps.setInt(1, id); ps.setString(2, hash); ps.executeUpdate();
        }
        con.commit();
        try {
            util.CorreoRecuperacion.enviar(correo, nombre, codigo);
            AuditoriaDAO.registrar(id, "(correo)", "Recuperación", "RECUPERACION_SOLICITADA", "Se envió un código temporal por correo");
            return ResultadoRecuperacion.SOLICITUD_ACEPTADA;
        } catch (Exception e) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE recuperacion_password SET usado_en=now() WHERE token_hash=?")) {
                ps.setString(1, hash); ps.executeUpdate();
            }
            con.commit();
            util.Log.error("No se pudo enviar correo de recuperación", e);
            return ResultadoRecuperacion.CORREO_NO_CONFIGURADO;
        }
    } catch (SQLException e) {
        util.Log.error("Solicitud de recuperación", e);
        return ResultadoRecuperacion.ERROR;
    }
}

/** Cambia la contraseña solo si el código vigente es correcto y no superó cinco intentos. */
public boolean restablecerPassword(String email, String codigo, String nuevaPassword) {
    String correo = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    if (codigo == null || !codigo.matches("[A-Za-z0-9_-]{40,50}") || nuevaPassword == null
            || util.Validador.password(nuevaPassword) != null
            || !util.ValidadorPassword.esSegura(nuevaPassword)) return false;
    String tokenHash = hashToken(codigo);
    String sql = "SELECT r.id, r.id_usuario FROM recuperacion_password r JOIN usuarios u ON u.id_usuario=r.id_usuario "
            + "WHERE lower(u.email)=? AND r.token_hash=? AND r.usado_en IS NULL AND r.expira_en>now() AND r.intentos<5 AND u.estado::text='Activo' FOR UPDATE";
    try (Connection con = ConexionBD.getConexion()) {
        if (con == null) return false;
        con.setAutoCommit(false);
        try {
            int idUsuario;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, correo); ps.setString(2, tokenHash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        try (PreparedStatement intento = con.prepareStatement("UPDATE recuperacion_password SET intentos=intentos+1 WHERE token_hash=? AND usado_en IS NULL AND intentos<5")) {
                            intento.setString(1, tokenHash); intento.executeUpdate();
                        }
                        con.commit(); return false;
                    }
                    idUsuario = rs.getInt(2);
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET password=?, intentos_fallidos=0, bloqueado_hasta=NULL, email_verificado=TRUE WHERE id_usuario=?")) {
                ps.setString(1, Seguridad.hashear(nuevaPassword)); ps.setInt(2, idUsuario); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE recuperacion_password SET usado_en=now() WHERE id_usuario=? AND usado_en IS NULL")) {
                ps.setInt(1, idUsuario); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE sesiones_recordadas SET revocado_en=now() WHERE id_usuario=? AND revocado_en IS NULL")) {
                ps.setInt(1, idUsuario); ps.executeUpdate();
            }
            con.commit();
            AuditoriaDAO.registrar(idUsuario, "(recuperación)", "Recuperación", "CONTRASENA_RESTABLECIDA", "Cambio mediante código de un solo uso");
            return true;
        } catch (SQLException | RuntimeException e) {
            con.rollback(); util.Log.error("Restablecimiento de contraseña", e); return false;
        } finally { con.setAutoCommit(true); }
    } catch (SQLException e) {
        util.Log.error("Restablecimiento de contraseña", e); return false;
    }
}

private static String hashToken(String token) {
    return Seguridad.sha256(token);
}

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

    String sql = "SELECT u.id_usuario, u.nombre_completo, u.usuario, u.email, "
            + "r.nombre AS rol, u.estado::text AS estado, u.email_verificado "
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
            u.setEmail(rs.getString("email"));
            u.setEmailVerificado(rs.getBoolean("email_verificado"));
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
            + "(nombre_completo, usuario, password, rol, estado, id_rol, email) "
            + "VALUES (?, ?, ?, ?::rol_usuario, ?::estado_general, "
            + "(SELECT id_rol FROM roles WHERE LOWER(nombre) = LOWER(?)), ?)";

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
        ps.setString(7, normalizarEmail(u.getEmail()));

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
            + "password = CASE WHEN ? = '' THEN password ELSE ? END, "
            + "rol = ?::rol_usuario, "
            + "estado = ?::estado_general, "
            + "id_rol = (SELECT id_rol FROM roles WHERE LOWER(nombre) = LOWER(?)), "
            + "email = ?, "
            + "email_verificado = CASE WHEN lower(btrim(coalesce(email,''))) = lower(btrim(coalesce(?,''))) "
            + "THEN email_verificado ELSE false END "
            + "WHERE id_usuario = ?";

    try (
        Connection con = ConexionBD.getConexion();
        PreparedStatement ps = con.prepareStatement(sql)
    ) {

        ps.setString(1, u.getNombreCompleto());
        ps.setString(2, u.getUsuario());
        String passwordNueva = u.getPassword() == null ? "" : u.getPassword();
        ps.setString(3, passwordNueva);
        ps.setString(4, passwordNueva.isEmpty() ? "" : hashSiHaceFalta(passwordNueva));
        ps.setString(5, u.getRol());
        ps.setString(6, u.getEstado());
        ps.setString(7, u.getRol());
        ps.setString(8, normalizarEmail(u.getEmail()));
        ps.setString(9, normalizarEmail(u.getEmail()));
        ps.setInt(10, u.getIdUsuario());

        boolean actualizado = ps.executeUpdate() > 0;
        if (actualizado) revocarSesionesUsuario(u.getIdUsuario());
        return actualizado;

    } catch (SQLException e) {
        util.Log.error(e);
        System.err.println("Error al actualizar usuario: " + e.getMessage());
        return false;
    }
}

private static String normalizarEmail(String email) {
    return email == null || email.isBlank() ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
}

private void revocarSesionesUsuario(int idUsuario) {
    try (Connection con = ConexionBD.getConexion()) {
        if (con == null) return;
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE sesiones_recordadas SET revocado_en=now() WHERE id_usuario=? AND revocado_en IS NULL")) {
            ps.setInt(1, idUsuario); ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE recuperacion_password SET usado_en=now() WHERE id_usuario=? AND usado_en IS NULL")) {
            ps.setInt(1, idUsuario); ps.executeUpdate();
        }
    } catch (SQLException e) {
        // La operación principal no depende de que la migración de sesiones se haya aplicado.
        util.Log.info("No se revocaron sesiones persistentes; verifique la migración de sesiones.");
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

        boolean actualizado = ps.executeUpdate() > 0;
        if (actualizado && !"Activo".equalsIgnoreCase(estado)) revocarSesionesUsuario(idUsuario);
        return actualizado;

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
