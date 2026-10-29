
package vistas;

import dao.UsuarioDAO;
import modelo.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class FrmLogin extends JFrame {

        private JTextField txtUsuario;
        private JPasswordField txtPassword;
        private JButton btnIngresar;
        private Timer animacionBoton;

        private final Color VERDE = new Color(21, 128, 61);
        private final Color VERDE_OSCURO = new Color(22, 101, 52);
        private final Color FONDO = new Color(241, 245, 249);
        private final Color TEXTO = new Color(30, 41, 59);
        private final Color GRIS = new Color(100, 116, 139);
        private final Color BORDE = new Color(203, 213, 225);

        public FrmLogin() {

                setTitle("Bodega La Peruanita - Acceso al Sistema POS");
                setSize(460, 490);
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                setLocationRelativeTo(null);
                setResizable(true);
                setExtendedState(JFrame.MAXIMIZED_BOTH);

                getContentPane().setBackground(FONDO);
                setLayout(new BorderLayout());

                crearInterfaz();
                configurarEventos();
        }

        private void crearInterfaz() {

                // ==============================
                // ENCABEZADO
                // ==============================

                JPanel pnlHeader = new JPanel();
                pnlHeader.setLayout(new FlowLayout(FlowLayout.CENTER, 18, 12));
                pnlHeader.setBackground(VERDE_OSCURO);
                pnlHeader.setBorder(
                                new EmptyBorder(4, 20, 4, 20));

                JLabel lblLogo = new JLabel();
                URL logoUrl = FrmLogin.class.getResource("/imagen/logo.png");
                ImageIcon logo = logoUrl != null
                                ? new ImageIcon(logoUrl)
                                : new ImageIcon("Sistema_pos_orign/src/imagen/logo.png");
                if (logo.getIconWidth() > 0) {
                        Image imagen = logo.getImage().getScaledInstance(76, 76, Image.SCALE_SMOOTH);
                        lblLogo.setIcon(new ImageIcon(imagen));
                        setIconImage(logo.getImage());
                }

                JPanel pnlMarca = new JPanel();
                pnlMarca.setLayout(new BoxLayout(pnlMarca, BoxLayout.Y_AXIS));
                pnlMarca.setOpaque(false);

                JLabel lblTitulo = new JLabel("BODEGA LA PERUANITA");

                lblTitulo.setFont(
                                new Font("Segoe UI", Font.BOLD, 22));
                lblTitulo.setForeground(Color.WHITE);

                JLabel lblSubtitulo = new JLabel("SISTEMA DE PUNTO DE VENTA");

                lblSubtitulo.setFont(
                                new Font("Segoe UI", Font.PLAIN, 11));
                lblSubtitulo.setForeground(new Color(220, 252, 231));

                pnlMarca.add(lblTitulo);
                pnlMarca.add(Box.createVerticalStrut(5));
                pnlMarca.add(lblSubtitulo);
                pnlHeader.add(lblLogo);
                pnlHeader.add(pnlMarca);

                add(pnlHeader, BorderLayout.NORTH);

                // ==============================
                // FORMULARIO
                // ==============================

                JPanel pnlContenedor = new PanelAparece();

                pnlContenedor.setBackground(FONDO);
                pnlContenedor.setBorder(
                                new EmptyBorder(18, 35, 20, 35));

                JPanel pnlFormulario = new JPanel(
                                new GridBagLayout());

                pnlFormulario.setBackground(Color.WHITE);
                pnlFormulario.setBorder(
                                BorderFactory.createCompoundBorder(
                                                new LineBorder(BORDE, 1, true),
                                                new EmptyBorder(22, 24, 22, 24)));

                GridBagConstraints gbc = new GridBagConstraints();

                gbc.gridx = 0;
                gbc.gridwidth = 1;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(4, 0, 4, 0);

                // Título del formulario
                JLabel lblBienvenida = new JLabel(
                                "¡Bienvenido!");

                lblBienvenida.setFont(
                                new Font("Segoe UI", Font.BOLD, 21));
                lblBienvenida.setForeground(TEXTO);

                gbc.gridy = 0;
                pnlFormulario.add(lblBienvenida, gbc);

                JLabel lblIndicacion = new JLabel(
                                "Ingresa tus credenciales para continuar.");

                lblIndicacion.setFont(
                                new Font("Segoe UI", Font.PLAIN, 11));
                lblIndicacion.setForeground(GRIS);

                gbc.gridy = 1;
                gbc.insets = new Insets(2, 0, 18, 0);
                pnlFormulario.add(lblIndicacion, gbc);

                // Etiqueta usuario
                JLabel lblUsuario = new JLabel("Usuario");

                lblUsuario.setFont(
                                new Font("Segoe UI", Font.BOLD, 12));
                lblUsuario.setForeground(TEXTO);

                gbc.gridy = 2;
                gbc.insets = new Insets(4, 0, 6, 0);
                pnlFormulario.add(lblUsuario, gbc);

                // Campo usuario
                txtUsuario = new JTextField();
                txtUsuario.setFont(
                                new Font("Segoe UI", Font.PLAIN, 14));
                txtUsuario.setForeground(TEXTO);
                txtUsuario.setBackground(Color.WHITE);
                txtUsuario.setCaretColor(VERDE);
                txtUsuario.setBorder(
                                BorderFactory.createCompoundBorder(
                                                new LineBorder(BORDE, 1, true),
                                                new EmptyBorder(10, 12, 10, 12)));
                txtUsuario.setPreferredSize(
                                new Dimension(280, 43));

                gbc.gridy = 3;
                gbc.insets = new Insets(0, 0, 15, 0);
                pnlFormulario.add(txtUsuario, gbc);

                // Etiqueta contraseña
                JLabel lblPassword = new JLabel("Contraseña");

                lblPassword.setFont(
                                new Font("Segoe UI", Font.BOLD, 12));
                lblPassword.setForeground(TEXTO);

                gbc.gridy = 4;
                gbc.insets = new Insets(4, 0, 6, 0);
                pnlFormulario.add(lblPassword, gbc);

                // Campo contraseña
                txtPassword = new JPasswordField();
                txtPassword.setFont(
                                new Font("Segoe UI", Font.PLAIN, 14));
                txtPassword.setForeground(TEXTO);
                txtPassword.setBackground(Color.WHITE);
                txtPassword.setCaretColor(VERDE);
                txtPassword.setBorder(new EmptyBorder(10, 12, 10, 8));
                txtPassword.setPreferredSize(new Dimension(220, 43));
                final char caracterOcultoPassword = txtPassword.getEchoChar();

                JPanel pnlCampoPassword = new JPanel(new BorderLayout());
                pnlCampoPassword.setBackground(Color.WHITE);
                pnlCampoPassword.setBorder(new LineBorder(BORDE, 1, true));
                pnlCampoPassword.setPreferredSize(new Dimension(280, 43));
                pnlCampoPassword.add(txtPassword, BorderLayout.CENTER);

                JToggleButton btnMostrarPassword = new JToggleButton("Mostrar");
                btnMostrarPassword.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                btnMostrarPassword.setForeground(VERDE_OSCURO);
                btnMostrarPassword.setBackground(Color.WHITE);
                btnMostrarPassword.setFocusPainted(false);
                btnMostrarPassword.setBorderPainted(false);
                btnMostrarPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnMostrarPassword.setPreferredSize(new Dimension(68, 41));
                btnMostrarPassword.addActionListener(e -> {
                        boolean mostrar = btnMostrarPassword.isSelected();
                        txtPassword.setEchoChar(mostrar ? (char) 0 : caracterOcultoPassword);
                        btnMostrarPassword.setText(mostrar ? "Ocultar" : "Mostrar");
                });
                pnlCampoPassword.add(btnMostrarPassword, BorderLayout.EAST);

                gbc.gridy = 5;
                gbc.insets = new Insets(0, 0, 20, 0);
                pnlFormulario.add(pnlCampoPassword, gbc);

                // Botón ingresar
                btnIngresar = new JButton("INICIAR SESIÓN");

                btnIngresar.setFont(
                                new Font("Segoe UI", Font.BOLD, 13));
                btnIngresar.setForeground(Color.WHITE);
                btnIngresar.setBackground(VERDE);
                btnIngresar.setFocusPainted(false);
                btnIngresar.setBorderPainted(false);
                btnIngresar.setOpaque(true);
                btnIngresar.setCursor(
                                new Cursor(Cursor.HAND_CURSOR));
                btnIngresar.setPreferredSize(
                                new Dimension(280, 45));

                gbc.gridy = 6;
                gbc.insets = new Insets(0, 0, 8, 0);
                pnlFormulario.add(btnIngresar, gbc);

                JLabel lblSeguridad = new JLabel(
                                "Acceso exclusivo para usuarios autorizados");

                lblSeguridad.setFont(
                                new Font("Segoe UI", Font.PLAIN, 10));
                lblSeguridad.setForeground(GRIS);
                lblSeguridad.setHorizontalAlignment(
                                SwingConstants.CENTER);

                gbc.gridy = 7;
                gbc.insets = new Insets(8, 0, 0, 0);
                pnlFormulario.add(lblSeguridad, gbc);

                pnlContenedor.add(pnlFormulario);

                add(pnlContenedor, BorderLayout.CENTER);

                // ==============================
                // PIE DE VENTANA
                // ==============================

                JPanel pnlFooter = new JPanel(
                                new FlowLayout(FlowLayout.CENTER, 5, 8));

                pnlFooter.setBackground(FONDO);

                JLabel lblFooter = new JLabel(
                                "Bodega La Peruanita  |  Sistema POS");

                lblFooter.setFont(
                                new Font("Segoe UI", Font.PLAIN, 10));
                lblFooter.setForeground(GRIS);

                pnlFooter.add(lblFooter);

                add(pnlFooter, BorderLayout.SOUTH);
        }

        private void configurarEventos() {

                configurarAnimacionCampo(txtUsuario, txtUsuario);
                configurarAnimacionCampo(txtPassword, (JComponent) txtPassword.getParent());

                // Transición suave de color al pasar el cursor
                btnIngresar.addMouseListener(new MouseAdapter() {

                        @Override
                        public void mouseEntered(MouseEvent e) {
                                animarColorBoton(VERDE_OSCURO);
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                                animarColorBoton(VERDE);
                        }
                });

                // Iniciar sesión al hacer clic
                btnIngresar.addActionListener(
                                (ActionEvent e) -> iniciarSesion());

                // También permite iniciar sesión con Enter
                getRootPane().setDefaultButton(btnIngresar);
        }

        private void animarColorBoton(Color destino) {
                if (animacionBoton != null && animacionBoton.isRunning()) {
                        animacionBoton.stop();
                }
                Color inicio = btnIngresar.getBackground();
                final int[] paso = {0};
                animacionBoton = new Timer(15, e -> {
                        paso[0]++;
                        float t = Math.min(1f, paso[0] / 12f);
                        btnIngresar.setBackground(new Color(
                                        (int) (inicio.getRed() + (destino.getRed() - inicio.getRed()) * t),
                                        (int) (inicio.getGreen() + (destino.getGreen() - inicio.getGreen()) * t),
                                        (int) (inicio.getBlue() + (destino.getBlue() - inicio.getBlue()) * t)));
                        if (t >= 1f) {
                                ((Timer) e.getSource()).stop();
                        }
                });
                animacionBoton.start();
        }

        private void configurarAnimacionCampo(JTextField campo, JComponent superficieBorde) {
                superficieBorde.putClientProperty("colorBorde", BORDE);
                campo.addFocusListener(new java.awt.event.FocusAdapter() {
                        @Override
                        public void focusGained(java.awt.event.FocusEvent e) {
                                animarBordeCampo(superficieBorde, VERDE);
                        }

                        @Override
                        public void focusLost(java.awt.event.FocusEvent e) {
                                animarBordeCampo(superficieBorde, BORDE);
                        }
                });
        }

        private void animarBordeCampo(JComponent superficieBorde, Color destino) {
                Object timerPrevio = superficieBorde.getClientProperty("animacionBorde");
                if (timerPrevio instanceof Timer) {
                        ((Timer) timerPrevio).stop();
                }
                Object colorActual = superficieBorde.getClientProperty("colorBorde");
                Color inicio = colorActual instanceof Color ? (Color) colorActual : BORDE;
                final int[] paso = {0};
                Timer timer = new Timer(15, e -> {
                        paso[0]++;
                        float t = Math.min(1f, paso[0] / 12f);
                        Color color = new Color(
                                        (int) (inicio.getRed() + (destino.getRed() - inicio.getRed()) * t),
                                        (int) (inicio.getGreen() + (destino.getGreen() - inicio.getGreen()) * t),
                                        (int) (inicio.getBlue() + (destino.getBlue() - inicio.getBlue()) * t));
                        if (superficieBorde instanceof JPanel) {
                                superficieBorde.setBorder(new LineBorder(color, 1, true));
                        } else {
                                superficieBorde.setBorder(BorderFactory.createCompoundBorder(
                                                new LineBorder(color, 1, true),
                                                new EmptyBorder(10, 12, 10, 12)));
                        }
                        superficieBorde.putClientProperty("colorBorde", color);
                        if (t >= 1f) {
                                ((Timer) e.getSource()).stop();
                        }
                });
                superficieBorde.putClientProperty("animacionBorde", timer);
                timer.start();
        }

        private static class PanelAparece extends JPanel {
                private float opacidad = 0f;

                PanelAparece() {
                        super(new GridBagLayout());
                }

                @Override
                public void addNotify() {
                        super.addNotify();
                        Timer timer = new Timer(15, e -> {
                                opacidad = Math.min(1f, opacidad + 0.06f);
                                repaint();
                                if (opacidad >= 1f) {
                                        ((Timer) e.getSource()).stop();
                                }
                        });
                        timer.start();
                }

                @Override
                public void paint(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setComposite(AlphaComposite.SrcOver.derive(opacidad));
                        super.paint(g2);
                        g2.dispose();
                }
        }

        private void iniciarSesion() {

                String usuario = txtUsuario.getText().trim();

                String password = new String(
                                txtPassword.getPassword()).trim();

                if (usuario.isEmpty() || password.isEmpty()) {

                        JOptionPane.showMessageDialog(
                                        this,
                                        "Por favor, completa el usuario y la contraseña.",
                                        "Campos obligatorios",
                                        JOptionPane.WARNING_MESSAGE);

                        return;
                }

                btnIngresar.setEnabled(false);
                btnIngresar.setText("VERIFICANDO...");

                try {

                        UsuarioDAO dao = new UsuarioDAO();

                        Usuario u = dao.autenticar(
                                        usuario,
                                        password);

                        if (u != null) {

                                JOptionPane.showMessageDialog(
                                                this,
                                                "Bienvenido(a) "
                                                                + u.getNombreCompleto()
                                                                + "\nRol: "
                                                                + u.getRol(),
                                                "Ingreso correcto",
                                                JOptionPane.INFORMATION_MESSAGE);

                                new FrmPrincipal(u).setVisible(true);
                                dispose();

                        } else {

                                JOptionPane.showMessageDialog(
                                                this,
                                                "Usuario o contraseña incorrectos.",
                                                "Acceso denegado",
                                                JOptionPane.ERROR_MESSAGE);

                                txtPassword.setText("");
                                txtPassword.requestFocus();
                        }

                } catch (Exception ex) {

                        JOptionPane.showMessageDialog(
                                        this,
                                        "No se pudo iniciar sesión.\n"
                                                        + "Detalle: " + ex.getMessage(),
                                        "Error de acceso",
                                        JOptionPane.ERROR_MESSAGE);

                } finally {

                        if (isDisplayable()) {
                                btnIngresar.setEnabled(true);
                                btnIngresar.setText("INICIAR SESIÓN");
                        }
                }
        }

        // MÉTODO MAIN
        public static void main(String args[]) {

                try {
                        UIManager.setLookAndFeel(
                                        UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                EventQueue.invokeLater(() -> {
                        new FrmLogin().setVisible(true);
                });
        }
}
