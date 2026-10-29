package universidad.sigelab.view;

import universidad.sigelab.controller.AutenticacionController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class LoginView extends JFrame {

    private static final Color AZUL_OSCURO = new Color(23, 54, 93);
    private static final Color AZUL_HOVER = new Color(39, 75, 119);
    private static final Color AZUL = new Color(37, 99, 235);
    private static final Color FONDO = new Color(245, 247, 251);
    private static final Color TEXTO = new Color(31, 41, 55);
    private static final Color TEXTO_SECUNDARIO = new Color(100, 116, 139);
    private static final Color BORDE = new Color(214, 223, 235);
    private static final Color BLANCO = Color.WHITE;

    // Se conserva el nombre requerido por LoginView.form.
    private JPanel Panelprincipal;

    private JTextField textUsuario;
    private JPasswordField textPassword;
    private JButton btIngresar;
    private JLabel lblEstado;

    private final AutenticacionController controladorAutenticacion =
            new AutenticacionController();

    public LoginView() {
        configurarVentana();
        inicializarComponentes();
        registrarEventos();
    }

    private void configurarVentana() {
        setTitle("SIGELAB - Inicio de sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 580));
        setSize(1100, 650);
        setLocationRelativeTo(null);
    }

    private void inicializarComponentes() {
        // Panelprincipal se mantiene como contenedor principal.
        Panelprincipal = new JPanel(new GridBagLayout());
        Panelprincipal.setBackground(FONDO);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        JPanel panelIzquierdo = crearPanelIzquierdo();
        gbc.gridx = 0;
        gbc.weightx = 0.37;
        Panelprincipal.add(panelIzquierdo, gbc);

        JPanel panelDerecho = crearPanelDerecho();
        gbc.gridx = 1;
        gbc.weightx = 0.63;
        Panelprincipal.add(panelDerecho, gbc);

        setContentPane(Panelprincipal);
    }

    private JPanel crearPanelIzquierdo() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(AZUL_OSCURO);
        panel.setBorder(new EmptyBorder(35, 30, 35, 30));

        JPanel contenido = new JPanel();
        contenido.setOpaque(false);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));

        JLabel lblLogo = new JLabel("SIGELAB");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 42));
        lblLogo.setForeground(BLANCO);
        lblLogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        contenido.add(lblLogo);
        contenido.add(Box.createVerticalStrut(12));

        JTextArea subtitulo = crearTextoAjustable(
                "SISTEMA DE GESTIÓN Y CONTROL DE LABORATORIOS",
                13,
                Font.BOLD,
                new Color(226, 235, 248)
        );

        contenido.add(subtitulo);
        contenido.add(Box.createVerticalStrut(28));

        JPanel linea = new JPanel();
        linea.setBackground(new Color(86, 113, 145));
        linea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        linea.setPreferredSize(new Dimension(250, 1));
        linea.setAlignmentX(Component.LEFT_ALIGNMENT);

        contenido.add(linea);
        contenido.add(Box.createVerticalStrut(30));

        JTextArea descripcion = crearTextoAjustable(
                "Administra los laboratorios, equipos, préstamos y "
                        + "devoluciones de forma centralizada y segura.",
                16,
                Font.PLAIN,
                new Color(230, 237, 247)
        );

        contenido.add(descripcion);
        contenido.add(Box.createVerticalStrut(32));

        JLabel lblSeguridad = new JLabel("ACCESO SEGURO");
        lblSeguridad.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSeguridad.setForeground(BLANCO);
        lblSeguridad.setOpaque(true);
        lblSeguridad.setBackground(AZUL_HOVER);
        lblSeguridad.setBorder(new EmptyBorder(12, 18, 12, 18));
        lblSeguridad.setAlignmentX(Component.LEFT_ALIGNMENT);

        contenido.add(lblSeguridad);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        panel.add(contenido, gbc);

        return panel;
    }

    private JPanel crearPanelDerecho() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(FONDO);
        panel.setBorder(new EmptyBorder(30, 35, 30, 35));

        JPanel tarjeta = new JPanel(new GridBagLayout());
        tarjeta.setBackground(BLANCO);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(34, 40, 30, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int fila = 0;

        JLabel lblBienvenido = new JLabel("Bienvenido");
        lblBienvenido.setFont(new Font("Segoe UI", Font.BOLD, 34));
        lblBienvenido.setForeground(TEXTO);
        agregarFila(tarjeta, gbc, fila++, lblBienvenido, 0);

        JLabel lblIndicacion = new JLabel(
                "Ingresa tus credenciales para continuar."
        );
        lblIndicacion.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblIndicacion.setForeground(TEXTO_SECUNDARIO);
        agregarFila(tarjeta, gbc, fila++, lblIndicacion, 12);

        JLabel lblUsuario = crearEtiqueta("Usuario");
        agregarFila(tarjeta, gbc, fila++, lblUsuario, 30);

        textUsuario = new JTextField();
        configurarCampo(textUsuario);
        textUsuario.setToolTipText("Ingresa tu nombre de usuario");
        agregarFila(tarjeta, gbc, fila++, textUsuario, 8);

        JLabel lblPassword = crearEtiqueta("Contraseña");
        agregarFila(tarjeta, gbc, fila++, lblPassword, 24);

        textPassword = new JPasswordField();
        configurarCampo(textPassword);
        textPassword.setToolTipText("Ingresa tu contraseña");
        agregarFila(tarjeta, gbc, fila++, textPassword, 8);

        btIngresar = new JButton("INGRESAR AL SISTEMA");
        btIngresar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btIngresar.setForeground(BLANCO);
        btIngresar.setBackground(AZUL);
        btIngresar.setOpaque(true);
        btIngresar.setBorderPainted(false);
        btIngresar.setFocusPainted(false);
        btIngresar.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
        ));
        btIngresar.setPreferredSize(new Dimension(200, 56));
        btIngresar.setBorder(new EmptyBorder(14, 18, 14, 18));

        btIngresar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btIngresar.isEnabled()) {
                    btIngresar.setBackground(new Color(29, 78, 216));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btIngresar.setBackground(AZUL);
            }
        });

        agregarFila(tarjeta, gbc, fila++, btIngresar, 28);

        JTextArea notaFacial = crearTextoAjustable(
                "El sistema puede solicitar una verificación facial "
                        + "durante el inicio de sesión.",
                12,
                Font.PLAIN,
                TEXTO_SECUNDARIO
        );
        agregarFila(tarjeta, gbc, fila++, notaFacial, 18);

        lblEstado = new JLabel(" ");
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEstado.setForeground(TEXTO_SECUNDARIO);
        agregarFila(tarjeta, gbc, fila, lblEstado, 8);

        GridBagConstraints tarjetaGbc = new GridBagConstraints();
        tarjetaGbc.gridx = 0;
        tarjetaGbc.gridy = 0;
        tarjetaGbc.weightx = 1;
        tarjetaGbc.weighty = 0;
        tarjetaGbc.fill = GridBagConstraints.HORIZONTAL;
        tarjetaGbc.anchor = GridBagConstraints.CENTER;

        panel.add(tarjeta, tarjetaGbc);

        return panel;
    }

    private void agregarFila(
            JPanel panel,
            GridBagConstraints gbc,
            int fila,
            Component componente,
            int espacioSuperior
    ) {
        gbc.gridy = fila;
        gbc.insets = new Insets(espacioSuperior, 0, 0, 0);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        panel.add(componente, (GridBagConstraints) gbc.clone());
    }

    private JTextArea crearTextoAjustable(
            String texto,
            int tamanio,
            int estilo,
            Color color
    ) {
        JTextArea area = new JTextArea(texto);
        area.setFont(new Font("Segoe UI", estilo, tamanio));
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setMargin(new Insets(0, 0, 0, 0));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 14));
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    private void configurarCampo(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        campo.setForeground(TEXTO);
        campo.setBackground(BLANCO);
        campo.setCaretColor(AZUL);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(10, 12, 10, 12)
        ));
        campo.setPreferredSize(new Dimension(200, 48));
    }

    private void registrarEventos() {
        btIngresar.addActionListener(e -> iniciarSesion());

        textUsuario.addActionListener(
                e -> textPassword.requestFocusInWindow()
        );

        textPassword.addActionListener(e -> iniciarSesion());

        SwingUtilities.invokeLater(
                () -> textUsuario.requestFocusInWindow()
        );
    }

    private void iniciarSesion() {
        String nombreUsuario = textUsuario.getText().trim();
        char[] caracteresPassword = textPassword.getPassword();
        String contrasena = new String(caracteresPassword);

        if (nombreUsuario.isEmpty() || contrasena.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ingresa tu usuario y contraseña.",
                    "Campos obligatorios",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        btIngresar.setEnabled(false);
        btIngresar.setText("VERIFICANDO...");
        lblEstado.setText("Validando credenciales...");

        SwingWorker<Usuario, Void> trabajador = new SwingWorker<>() {
            @Override
            protected Usuario doInBackground() {
                return controladorAutenticacion.iniciarSesion(
                        nombreUsuario,
                        contrasena
                );
            }

            @Override
            protected void done() {
                try {
                    Usuario usuario = get();

                    if (usuario != null) {
                        new PrincipalFrame(usuario).setVisible(true);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(
                                LoginView.this,
                                "No fue posible iniciar sesión. "
                                        + "Verifica tus credenciales.",
                                "Acceso denegado",
                                JOptionPane.WARNING_MESSAGE
                        );
                    }

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();

                    JOptionPane.showMessageDialog(
                            LoginView.this,
                            "El inicio de sesión fue interrumpido.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );

                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause() != null
                            ? ex.getCause()
                            : ex;

                    JOptionPane.showMessageDialog(
                            LoginView.this,
                            ManejadorErrores.mensajeParaUsuario(causa),
                            "Error al iniciar sesión",
                            JOptionPane.ERROR_MESSAGE
                    );

                } finally {
                    btIngresar.setEnabled(true);
                    btIngresar.setText("INGRESAR AL SISTEMA");
                    textPassword.setText("");
                    lblEstado.setText(" ");
                }
            }
        };

        trabajador.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginView login = new LoginView();
            login.setVisible(true);
        });
    }
}
