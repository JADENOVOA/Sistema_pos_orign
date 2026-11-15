package vistas;

import dao.AuditoriaDAO;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import modelo.Usuario;
import util.IntegridadArchivos;
import util.Permisos;
import util.RespaldoBD;

/** Módulo de Seguridad (solo Administrador): auditoría, respaldos/recuperación e integridad de archivos. */
public class PnlSeguridad extends JPanel {

    private final Usuario usuario;
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    private final Color VERDE = new Color(21, 128, 61);
    private final Color FONDO = new Color(241, 245, 249);
    private final Color BORDE = new Color(226, 232, 240);

    private DefaultTableModel modAuditoria, modRespaldos, modIntegridad;
    private JTable tblRespaldos;
    private JTextField txtFiltro;
    private JLabel lblEstadoRespaldo;

    private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public PnlSeguridad(Usuario usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout());
        setBackground(FONDO);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("Registro de auditoría", crearTabAuditoria());
        tabs.addTab("Respaldos y recuperación", crearTabRespaldos());
        tabs.addTab("Integridad de archivos", crearTabIntegridad());
        add(tabs, BorderLayout.CENTER);
    }

    public void refrescarDatos() {
        cargarAuditoria();
        cargarRespaldos();
        cargarIntegridad();
    }

    // ------------------------------------------------------------------ utilidades UI
    private JButton boton(String texto, Color fondo) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(fondo);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Renderer que NUNCA interpreta HTML (evita que un texto como <html>... se ejecute/renderice). */
    private JTable tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (c instanceof JComponent) ((JComponent) c).putClientProperty("html.disable", Boolean.TRUE);
                return c;
            }
        };
        t.setRowHeight(26);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setAutoCreateRowSorter(true);
        return t;
    }

    private DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JPanel contenedor(Component norte, Component centro) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(BORDE, 1, true), new EmptyBorder(12, 12, 12, 12)));
        p.add(norte, BorderLayout.NORTH);
        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    private boolean autorizado(String accion) {
        return Permisos.autorizar(this, usuario, Permisos.Modulo.SEGURIDAD, accion);
    }

    // ------------------------------------------------------------------ AUDITORÍA
    private JPanel crearTabAuditoria() {
        modAuditoria = modelo("N°", "Fecha y hora", "Usuario", "Módulo", "Acción", "Detalle", "Equipo");
        JTable t = tabla(modAuditoria);

        txtFiltro = new JTextField(22);
        JButton btnBuscar = boton("Buscar / Actualizar", VERDE);
        btnBuscar.addActionListener(e -> { if (autorizado("Consultar auditoría")) cargarAuditoria(); });

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        barra.add(new JLabel("Filtrar (usuario, módulo, acción):"));
        barra.add(txtFiltro);
        barra.add(btnBuscar);
        barra.add(new JLabel("   Los registros son de solo lectura: no se pueden editar ni eliminar."));
        return contenedor(barra, new JScrollPane(t));
    }

    private void cargarAuditoria() {
        modAuditoria.setRowCount(0);
        for (Object[] f : auditoriaDAO.listar(500, txtFiltro == null ? "" : txtFiltro.getText())) {
            f[1] = fmt.format((java.util.Date) f[1]);
            modAuditoria.addRow(f);
        }
    }

    // ------------------------------------------------------------------ RESPALDOS
    private JPanel crearTabRespaldos() {
        modRespaldos = modelo("Archivo de respaldo", "Fecha", "Tamaño");
        tblRespaldos = tabla(modRespaldos);

        JButton btnGenerar = boton("Generar respaldo ahora", VERDE);
        JButton btnRestaurar = boton("Restaurar seleccionado…", new Color(180, 83, 9));
        JButton btnActualizar = boton("Actualizar lista", new Color(71, 85, 105));
        btnGenerar.addActionListener(e -> generarRespaldo());
        btnRestaurar.addActionListener(e -> restaurarRespaldo());
        btnActualizar.addActionListener(e -> cargarRespaldos());

        lblEstadoRespaldo = new JLabel(" ");
        lblEstadoRespaldo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        barra.add(btnGenerar);
        barra.add(btnRestaurar);
        barra.add(btnActualizar);

        JPanel norte = new JPanel(new BorderLayout(0, 6));
        norte.setOpaque(false);
        norte.add(barra, BorderLayout.NORTH);
        norte.add(lblEstadoRespaldo, BorderLayout.CENTER);
        JLabel info = new JLabel("<html>Carpeta: " + util.Validador.escaparHtml(RespaldoBD.carpetaRespaldos().getAbsolutePath())
                + " &nbsp;|&nbsp; Respaldo automático al iniciar (si el último tiene más de 24 h) y luego cada 24 h.</html>");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        norte.add(info, BorderLayout.SOUTH);
        return contenedor(norte, new JScrollPane(tblRespaldos));
    }

    private void cargarRespaldos() {
        modRespaldos.setRowCount(0);
        for (File f : RespaldoBD.listar()) {
            modRespaldos.addRow(new Object[] { f.getName(), fmt.format(new Date(f.lastModified())),
                RespaldoBD.formatoTamano(f.length()) });
        }
    }

    private void generarRespaldo() {
        if (!autorizado("Generar respaldo")) return;
        lblEstadoRespaldo.setText("Generando respaldo, espere…");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<RespaldoBD.Resultado, Void>() {
            @Override protected RespaldoBD.Resultado doInBackground() {
                return RespaldoBD.generar(usuario, "Respaldo manual");
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    RespaldoBD.Resultado r = get();
                    lblEstadoRespaldo.setText(r.mensaje);
                    lblEstadoRespaldo.setForeground(r.ok ? VERDE : new Color(185, 28, 28));
                    JOptionPane.showMessageDialog(PnlSeguridad.this, r.mensaje, "Respaldo",
                            r.ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    util.Log.error("Respaldo manual", ex);
                    JOptionPane.showMessageDialog(PnlSeguridad.this, util.Log.MENSAJE_GENERICO,
                            "Respaldo", JOptionPane.ERROR_MESSAGE);
                }
                cargarRespaldos();
            }
        }.execute();
    }

    private void restaurarRespaldo() {
        if (!autorizado("Restaurar respaldo")) return;
        int fila = tblRespaldos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un respaldo de la lista.");
            return;
        }
        final File archivo = new File(RespaldoBD.carpetaRespaldos(),
                modRespaldos.getValueAt(tblRespaldos.convertRowIndexToModel(fila), 0).toString());
        String sugerida = conexion.ConexionBD.getNombreBD() + "_PRUEBA";
        String destino = JOptionPane.showInputDialog(this,
                "Base de datos donde se restaurará (se crea si no existe).\n"
                + "Para probar sin riesgo use una base distinta a la de producción:",
                sugerida);
        if (destino == null) return;
        destino = destino.trim();
        if (destino.equalsIgnoreCase(conexion.ConexionBD.getNombreBD())) {
            String confirma = JOptionPane.showInputDialog(this,
                    "ATENCIÓN: va a SOBRESCRIBIR la base de producción.\nEscriba el nombre de la base para confirmar:");
            if (confirma == null || !confirma.trim().equals(conexion.ConexionBD.getNombreBD())) {
                JOptionPane.showMessageDialog(this, "Restauración cancelada.");
                return;
            }
            RespaldoBD.generar(usuario, "Respaldo de seguridad previo a restaurar");
        }
        final String base = destino;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<RespaldoBD.Resultado, Void>() {
            @Override protected RespaldoBD.Resultado doInBackground() {
                return RespaldoBD.restaurar(usuario, archivo, base);
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    RespaldoBD.Resultado r = get();
                    JOptionPane.showMessageDialog(PnlSeguridad.this, r.mensaje, "Recuperación",
                            r.ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    util.Log.error("Restauración", ex);
                    JOptionPane.showMessageDialog(PnlSeguridad.this, util.Log.MENSAJE_GENERICO,
                            "Recuperación", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------ INTEGRIDAD
    private JPanel crearTabIntegridad() {
        modIntegridad = modelo("Archivo", "SHA-256 actual", "Estado");
        JTable t = tabla(modIntegridad);
        t.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tb, v, s, f, r, c);
                if (!s) comp.setForeground("OK".equals(String.valueOf(v)) ? VERDE : new Color(185, 28, 28));
                return comp;
            }
        });

        JButton btnVerificar = boton("Verificar ahora", VERDE);
        JButton btnBase = boton("Registrar estado actual como línea base", new Color(71, 85, 105));
        btnVerificar.addActionListener(e -> { if (autorizado("Verificar integridad")) cargarIntegridad(); });
        btnBase.addActionListener(e -> {
            if (!autorizado("Generar línea base de integridad")) return;
            if (JOptionPane.showConfirmDialog(this,
                    "Use esta opción solo si los archivos cambiaron de forma legítima (actualización).\n¿Continuar?",
                    "Línea base", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try {
                IntegridadArchivos.generarLineaBase(usuario);
                cargarIntegridad();
            } catch (Exception ex) {
                util.Log.error("Línea base", ex);
                JOptionPane.showMessageDialog(this, util.Log.MENSAJE_GENERICO);
            }
        });

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        barra.add(btnVerificar);
        barra.add(btnBase);
        barra.add(new JLabel("  Si un archivo cambia, su hash SHA-256 es distinto al de la línea base."));
        return contenedor(barra, new JScrollPane(t));
    }

    private void cargarIntegridad() {
        modIntegridad.setRowCount(0);
        List<IntegridadArchivos.Fila> filas = IntegridadArchivos.verificar();
        for (IntegridadArchivos.Fila f : filas) {
            modIntegridad.addRow(new Object[] { f.ruta, f.hashActual.isEmpty() ? f.hashBase : f.hashActual, f.estado.name() });
        }
    }
}
