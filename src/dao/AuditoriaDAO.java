package dao;

import conexion.ConexionBD;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import modelo.Usuario;

public class AuditoriaDAO {

    private static String equipo;

    private static String equipo() {
        if (equipo == null) {
            try { equipo = InetAddress.getLocalHost().getHostName(); }
            catch (Exception e) { equipo = "desconocido"; }
        }
        return equipo;
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Registra una operación del usuario con sesión iniciada. Nunca lanza excepciones. */
    public static void registrar(Usuario u, String modulo, String accion, String detalle) {
        registrar(u == null ? null : u.getIdUsuario(),
                u == null ? "(sin sesión)" : u.getUsuario(), modulo, accion, detalle);
    }

    /** Registra una operación (también sirve para intentos de login de usuarios sin sesión). */
    public static void registrar(Integer idUsuario, String usuario, String modulo, String accion, String detalle) {
        String sql = "INSERT INTO auditoria (id_usuario, usuario, modulo, accion, detalle, equipo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getConexion()) {
            if (con == null) return;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (idUsuario == null || idUsuario <= 0) ps.setNull(1, java.sql.Types.INTEGER);
                else ps.setInt(1, idUsuario);
                ps.setString(2, recortar(usuario == null ? "(desconocido)" : usuario, 60));
                ps.setString(3, recortar(modulo, 40));
                ps.setString(4, recortar(accion, 60));
                ps.setString(5, recortar(detalle, 255));
                ps.setString(6, recortar(equipo(), 100));
                ps.executeUpdate();
            }
        } catch (SQLException | RuntimeException e) {
            util.Log.error("No se pudo registrar la auditoría", e);
        }
    }

    /** Últimos registros (más recientes primero). filtro opcional sobre usuario/módulo/acción/detalle. */
    public List<Object[]> listar(int limite, String filtro) {
        List<Object[]> filas = new ArrayList<>();
        String sql = "SELECT id_auditoria, fecha_hora, usuario, modulo, accion, detalle, equipo "
                + "FROM auditoria WHERE (? = '' OR LOWER(usuario) LIKE ? OR LOWER(modulo) LIKE ? "
                + "OR LOWER(accion) LIKE ? OR LOWER(COALESCE(detalle,'')) LIKE ?) "
                + "ORDER BY id_auditoria DESC LIMIT ?";
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        String like = "%" + f.replace("%", "").replace("_", "") + "%";
        try (Connection con = ConexionBD.getConexion()) {
            if (con == null) return filas;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, f);
                ps.setString(2, like); ps.setString(3, like); ps.setString(4, like); ps.setString(5, like);
                ps.setInt(6, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        filas.add(new Object[] {
                            rs.getInt(1), rs.getTimestamp(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getString(7) });
                    }
                }
            }
        } catch (SQLException | RuntimeException e) {
            util.Log.error("No se pudo listar la auditoría", e);
        }
        return filas;
    }

    public static void crearAlerta(String tipo, String usuarioAfectado, String mensaje) {
        String sql = "INSERT INTO alertas_seguridad (tipo, usuario_afectado, mensaje) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.getConexion()) {
            if (con == null) return;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, recortar(tipo, 40));
                ps.setString(2, recortar(usuarioAfectado, 60));
                ps.setString(3, recortar(mensaje, 255));
                ps.executeUpdate();
            }
        } catch (SQLException | RuntimeException e) {
            util.Log.error("No se pudo crear la alerta de seguridad", e);
        }
    }

    public List<Object[]> alertasPendientes() {
        List<Object[]> filas = new ArrayList<>();
        String sql = "SELECT id_alerta, fecha_hora, tipo, usuario_afectado, mensaje "
                + "FROM alertas_seguridad WHERE atendida = FALSE ORDER BY id_alerta";
        try (Connection con = ConexionBD.getConexion()) {
            if (con == null) return filas;
            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[] { rs.getInt(1), rs.getTimestamp(2), rs.getString(3),
                        rs.getString(4), rs.getString(5) });
                }
            }
        } catch (SQLException | RuntimeException e) {
            util.Log.error("No se pudieron leer las alertas", e);
        }
        return filas;
    }

    public void marcarAtendida(int idAlerta) {
        try (Connection con = ConexionBD.getConexion()) {
            if (con == null) return;
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE alertas_seguridad SET atendida = TRUE WHERE id_alerta = ?")) {
                ps.setInt(1, idAlerta);
                ps.executeUpdate();
            }
        } catch (SQLException | RuntimeException e) {
            util.Log.error("No se pudo marcar la alerta", e);
        }
    }

    public static Timestamp ahora() { return new Timestamp(System.currentTimeMillis()); }
}
