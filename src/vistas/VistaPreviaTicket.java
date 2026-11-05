
package vistas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.*;

public class VistaPreviaTicket extends JDialog {

    private JTextArea areaTicket;
    private JPanel panelPapel;
    private JPanel fondo;
    private JComboBox<String> cmbPapel;

    public VistaPreviaTicket(Window propietario, String textoTicket) {

        super(propietario, "Vista previa de impresión",
                ModalityType.APPLICATION_MODAL);

        setSize(1000, 750);
        setMinimumSize(new Dimension(850, 650));
        setLocationRelativeTo(propietario);
        setLayout(new BorderLayout());

        getContentPane().setBackground(new Color(241, 245, 249));

        // ==============================
        // CABECERA
        // ==============================

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(Color.WHITE);
        cabecera.setBorder(new EmptyBorder(18, 25, 18, 25));

        JLabel titulo = new JLabel("VISTA PREVIA DE IMPRESIÓN");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(22, 101, 52));

        JLabel descripcion = new JLabel(
                "Revisa cómo quedará tu comprobante antes de imprimirlo.");
        descripcion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descripcion.setForeground(new Color(100, 116, 139));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 5));
        textos.setBackground(Color.WHITE);
        textos.add(titulo);
        textos.add(descripcion);

        cabecera.add(textos, BorderLayout.CENTER);
        add(cabecera, BorderLayout.NORTH);

        // ==============================
        // VISTA PREVIA DEL TICKET
        // ==============================

        fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(new Color(226, 232, 240));
        fondo.setBorder(new EmptyBorder(25, 25, 25, 25));

        areaTicket = new JTextArea(textoTicket);
        areaTicket.setEditable(false);
        areaTicket.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaTicket.setForeground(Color.BLACK);
        areaTicket.setBackground(Color.WHITE);
        areaTicket.setMargin(new Insets(25, 20, 25, 20));
        areaTicket.setLineWrap(false);
        areaTicket.setWrapStyleWord(false);
        areaTicket.setCaretPosition(0);

        panelPapel = new JPanel(new BorderLayout());
        panelPapel.setBackground(Color.WHITE);

        panelPapel.setBorder(
                BorderFactory.createLineBorder(
                        new Color(203, 213, 225), 1));

        panelPapel.add(areaTicket, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(panelPapel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(
                new Color(226, 232, 240));
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        // IMPORTANTE: hacer que el JScrollPane ocupe todo el espacio.
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        fondo.add(scroll, gbc);

        add(fondo, BorderLayout.CENTER);

        // ==============================
        // PANEL INFERIOR
        // ==============================

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBackground(Color.WHITE);
        inferior.setBorder(new EmptyBorder(15, 25, 15, 25));

        JPanel opciones = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 5));
        opciones.setBackground(Color.WHITE);

        JLabel lblPapel = new JLabel("Tamaño del papel:");
        lblPapel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        cmbPapel = new JComboBox<>(new String[] {
                "80 mm",
                "58 mm"
        });

        cmbPapel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbPapel.setPreferredSize(new Dimension(100, 32));

        opciones.add(lblPapel);
        opciones.add(cmbPapel);
        // BOTÓN CERRAR
        JButton btnCerrar = new JButton("CERRAR");
        btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrar.setPreferredSize(new Dimension(110, 38));
        btnCerrar.setBackground(new Color(22, 101, 52));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.setFocusPainted(false);
        btnCerrar.setBorderPainted(false);
        btnCerrar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // BOTÓN IMPRIMIR
        JButton btnImprimir = new JButton("IMPRIMIR");
        btnImprimir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnImprimir.setPreferredSize(new Dimension(120, 38));
        btnImprimir.setBackground(new Color(21, 128, 61));
        btnImprimir.setForeground(Color.WHITE);
        btnImprimir.setFocusPainted(false);
        btnImprimir.setBorderPainted(false);
        btnImprimir.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setBackground(Color.WHITE);
        botones.add(btnCerrar);
        botones.add(btnImprimir);

        inferior.add(opciones, BorderLayout.WEST);
        inferior.add(botones, BorderLayout.EAST);

        add(inferior, BorderLayout.SOUTH);

        // ==============================
        // EVENTOS
        // ==============================

        cmbPapel.addActionListener(e -> actualizarVista());

        btnCerrar.addActionListener(e -> dispose());

        btnImprimir.addActionListener(e -> imprimirTicket());

        actualizarVista();
    }

    // ==============================
    // ACTUALIZAR VISTA PREVIA
    // ==============================

    private void actualizarVista() {

        int ancho;

        if (cmbPapel.getSelectedIndex() == 0) {
            ancho = 450;
        } else {
            ancho = 330;
        }

        // Calculamos la altura según la cantidad de líneas.
        int cantidadLineas = areaTicket.getLineCount();

        int alto = Math.max(650, cantidadLineas * 19 + 60);

        panelPapel.setPreferredSize(
                new Dimension(ancho, alto));

        panelPapel.setMinimumSize(
                new Dimension(ancho, alto));

        areaTicket.setFont(
                new Font("Monospaced", Font.PLAIN, 12));

        panelPapel.revalidate();
        fondo.revalidate();
        fondo.repaint();
    }

    // ==============================
    // IMPRESIÓN NATIVA DE WINDOWS
    // ==============================

    private void imprimirTicket() {

        try {

            PrinterJob trabajo = PrinterJob.getPrinterJob();

            PageFormat formato = trabajo.defaultPage();

            Paper papel = new Paper();

            double ancho = cmbPapel.getSelectedIndex() == 0
                    ? 80 * 72 / 25.4
                    : 58 * 72 / 25.4;

            double margen = 3 * 72 / 25.4;
            double alto = 2000 * 72 / 25.4;

            papel.setSize(ancho, alto);

            papel.setImageableArea(
                    margen,
                    margen,
                    ancho - margen * 2,
                    alto - margen * 2);

            formato.setPaper(papel);
            formato.setOrientation(PageFormat.PORTRAIT);

            trabajo.setPrintable((grafico, pagina, indice) -> {

                if (indice > 0) {
                    return Printable.NO_SUCH_PAGE;
                }

                Graphics2D g2 = (Graphics2D) grafico.create();

                g2.translate(
                        formato.getImageableX(),
                        formato.getImageableY());

                g2.setFont(
                        new Font("Monospaced", Font.PLAIN, 8));

                FontMetrics fm = g2.getFontMetrics();

                String[] lineas = areaTicket.getText().split("\n");

                int y = fm.getAscent();

                for (String linea : lineas) {

                    g2.drawString(linea, 0, y);

                    y += fm.getHeight();
                }

                g2.dispose();

                return Printable.PAGE_EXISTS;

            }, formato);

            // Windows mostrará su cuadro de impresión.
            // No se imprime nada si el usuario cancela.

            if (trabajo.printDialog()) {

                trabajo.print();

                JOptionPane.showMessageDialog(
                        this,
                        "El ticket fue enviado a la impresora.",
                        "Impresión",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (PrinterException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo imprimir el ticket:\n"
                            + ex.getMessage(),
                    "Error de impresión",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
