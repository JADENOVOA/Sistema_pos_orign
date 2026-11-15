package util;

import dao.AuditoriaDAO;
import dao.UsuarioDAO;
import java.awt.Component;
import javax.swing.JOptionPane;
import modelo.Usuario;

public final class Permisos {

    public enum Modulo {
        INVENTARIO("Inventario", false), INVENTARIO_EDICION("Inventario (modificar)", true), REPORTES("Reportes", false),
        VENTAS("Ventas", false), PRODUCTOS("Productos", false), CAJA("Caja", false),
        USUARIOS("Usuarios", true), SEGURIDAD("Seguridad", true);

        private final String nombre;
        private final boolean soloAdmin;
        Modulo(String nombre, boolean soloAdmin) { this.nombre = nombre; this.soloAdmin = soloAdmin; }
        public String getNombre() { return nombre; }
        public boolean esSoloAdmin() { return soloAdmin; }
    }

    private static Runnable alSesionInvalida;

    private Permisos() {}

    /** FrmPrincipal registra aquí cómo cerrar la sesión cuando la cuenta fue desactivada. */
    public static void setAlSesionInvalida(Runnable r) { alSesionInvalida = r; }

    /** Permiso según la matriz (sin consultar la BD). */
    public static boolean permite(String rol, Modulo modulo) {
        if (rol == null) return false;
        if ("Administrador".equalsIgnoreCase(rol)) return true;
        return "Cajero".equalsIgnoreCase(rol) && !modulo.esSoloAdmin();
    }

    /**
     * Valida, justo antes de ejecutar la acción, que la cuenta siga ACTIVA y que su rol
     * actual en la BD tenga permiso sobre el módulo. Muestra el mensaje y audita si se deniega.
     */
    public static boolean autorizar(Component padre, Usuario u, Modulo modulo, String accion) {
        if (u == null) {
            avisar(padre, "Acceso denegado. Inicie sesión nuevamente.");
            return false;
        }
        String[] actual = new UsuarioDAO().obtenerRolYEstado(u.getIdUsuario());
        if (actual == null) {
            avisar(padre, Log.MENSAJE_GENERICO);
            return false;
        }
        if (!"Activo".equalsIgnoreCase(actual[1])) {
            AuditoriaDAO.registrar(u, modulo.getNombre(), "ACCESO_DENEGADO_CUENTA_INACTIVA", accion);
            if (alSesionInvalida != null) {
                alSesionInvalida.run(); // muestra el aviso y cierra la sesión
            } else {
                avisar(padre, "Su cuenta fue desactivada por un administrador.");
            }
            return false;
        }
        if (!permite(actual[0], modulo)) {
            AuditoriaDAO.registrar(u, modulo.getNombre(), "ACCESO_DENEGADO",
                    "Rol " + actual[0] + " intentó: " + accion);
            avisar(padre, "Acceso denegado: su rol (" + actual[0] + ") no tiene permiso para esta acción.");
            return false;
        }
        return true;
    }

    private static void avisar(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Acceso denegado", JOptionPane.WARNING_MESSAGE);
    }
}
