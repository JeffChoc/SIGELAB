
package universidad.sigelab.view;

import universidad.sigelab.controller.AutenticacionController;
import universidad.sigelab.controller.DashboardController;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.DashboardResumen;
import universidad.sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PrincipalFrame extends JFrame {

    private static final Color COLOR_ALERTA = new Color(0xB00020);
    private static final Color AZUL_LATERAL = new Color(23, 54, 93);
    private static final Color AZUL_HOVER = new Color(39, 75, 119);
    private static final Color TEXTO_LATERAL = new Color(235, 241, 249);

    private final Usuario usuario;

    private final AutenticacionController controladorAutenticacion =
            new AutenticacionController();

    private final DashboardController controladorDashboard =
            new DashboardController();

    private final Map<String, JFrame> ventanasAbiertas = new HashMap<>();

    private JPanel panelLateral;

    // Estadísticas del dashboard
    private final JLabel valorTotal = crearValor();
    private final JLabel valorDisponibles = crearValor();
    private final JLabel valorPrestados = crearValor();
    private final JLabel valorDanados = crearValor();
    private final JLabel valorMantenimiento = crearValor();
    private final JLabel valorBaja = crearValor();
    private final JLabel valorPrestamosActivos = crearValor();
    private final JLabel valorPrestamosVencidos = crearValor();

    public PrincipalFrame(Usuario usuario) {
        super("SIGELAB | Sistema de Gestión de Laboratorios");

        this.usuario = usuario;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        construirInterfaz();
        refrescarDashboard(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent evento) {
                refrescarDashboard(false);
            }
        });

        setMinimumSize(new Dimension(1000, 650));
        setSize(1120, 720);
        setLocationRelativeTo(null);

        // Aplicar el estilo general sin perder el formato de las cifras.
        EstiloSIGELAB.aplicar(this);
        aplicarEstiloLateral(panelLateral);
        aplicarEstiloNumeros();
    }

    // --------------------------------------------------
    // ESTRUCTURA PRINCIPAL
    // --------------------------------------------------

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(EstiloSIGELAB.FONDO);

        panelLateral = construirBarraLateral();

        raiz.add(panelLateral, BorderLayout.WEST);
        raiz.add(construirDashboard(), BorderLayout.CENTER);

        setContentPane(raiz);
    }

    // --------------------------------------------------
    // PANEL LATERAL
    // --------------------------------------------------

    private JPanel construirBarraLateral() {
        JPanel lateral = new JPanel();

        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBackground(AZUL_LATERAL);
        lateral.setPreferredSize(new Dimension(224, 650));
        lateral.setBorder(new EmptyBorder(22, 14, 18, 14));

        JLabel marca = new JLabel("SIGELAB");
        marca.setForeground(Color.WHITE);
        marca.setFont(new Font("Segoe UI", Font.BOLD, 25));
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("GESTIÓN DE LABORATORIOS");
        subtitulo.setForeground(new Color(190, 207, 228));
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        lateral.add(marca);
        lateral.add(Box.createVerticalStrut(3));
        lateral.add(subtitulo);
        lateral.add(Box.createVerticalStrut(27));
        lateral.add(crearSeparadorLateral());
        lateral.add(Box.createVerticalStrut(16));

        agregarSeccion(lateral, "GENERAL");

        agregarBotonLateral(
                lateral,
                "▦   Panel principal",
                this::enfocarPrincipal
        );

        RolUsuario rol = usuario.getRol().getNombre();

        boolean esAdmin = rol == RolUsuario.ADMIN;
        boolean puedeOperar =
                esAdmin || rol == RolUsuario.ENCARGADO;

        agregarEspacio(lateral, 14);
        agregarSeccion(lateral, "CATÁLOGOS");

        agregarBotonLateral(
                lateral,
                "▣   Equipos",
                () -> abrirVentana(
                        "equipos",
                        () -> new EquiposView(usuario)
                )
        );

        if (esAdmin) {
            agregarBotonLateral(
                    lateral,
                    "⌂   Laboratorios",
                    () -> abrirVentana(
                            "laboratorios",
                            LaboratorioView::new
                    )
            );

            agregarBotonLateral(
                    lateral,
                    "▤   Tipos de equipo",
                    () -> abrirVentana(
                            "tipos",
                            TipoEquipoView::new
                    )
            );

            agregarBotonLateral(
                    lateral,
                    "♙   Usuarios",
                    () -> abrirVentana(
                            "usuarios",
                            UsuarioView::new
                    )
            );
        }

        if (puedeOperar) {
            agregarEspacio(lateral, 14);
            agregarSeccion(lateral, "OPERACIONES");

            agregarBotonLateral(
                    lateral,
                    "⇄   Préstamos",
                    () -> abrirVentana(
                            "prestamos",
                            PrestamoView::new
                    )
            );

            agregarBotonLateral(
                    lateral,
                    "↩   Devoluciones",
                    () -> abrirVentana(
                            "devoluciones",
                            DevolucionView::new
                    )
            );

            agregarEspacio(lateral, 14);
            agregarSeccion(lateral, "INFORMACIÓN");

            agregarBotonLateral(
                    lateral,
                    "▥   Reportes",
                    () -> abrirVentana(
                            "reportes",
                            ReporteFrame::new
                    )
            );

            if (esAdmin) {
                agregarBotonLateral(
                        lateral,
                        "◷   Auditoría",
                        () -> abrirVentana(
                                "auditoria",
                                AuditoriaView::new
                        )
                );
            }
        }

        // Usuario y rol en la parte inferior
        lateral.add(Box.createVerticalGlue());
        lateral.add(crearSeparadorLateral());
        lateral.add(Box.createVerticalStrut(14));

        JLabel usuarioLabel =
                new JLabel("●  " + usuario.getNombreCompleto());

        usuarioLabel.setForeground(Color.WHITE);
        usuarioLabel.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );
        usuarioLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rolLabel =
                new JLabel("    " + String.valueOf(usuario.getRol()));

        rolLabel.setForeground(new Color(190, 207, 228));
        rolLabel.setFont(
                new Font("Segoe UI", Font.PLAIN, 11)
        );
        rolLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        lateral.add(usuarioLabel);
        lateral.add(Box.createVerticalStrut(4));
        lateral.add(rolLabel);
        lateral.add(Box.createVerticalStrut(13));

        agregarBotonLateral(
                lateral,
                "⇥   Cerrar sesión",
                this::cerrarSesion
        );

        return lateral;
    }

    // --------------------------------------------------
    // DASHBOARD
    // --------------------------------------------------

    private JPanel construirDashboard() {
        JPanel contenido = new JPanel(new BorderLayout(0, 22));

        contenido.setBackground(EstiloSIGELAB.FONDO);
        contenido.setBorder(
                new EmptyBorder(28, 30, 28, 30)
        );

        // Encabezado
        JPanel encabezado = new JPanel(
                new BorderLayout(10, 8)
        );
        encabezado.setOpaque(false);

        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(
                new BoxLayout(titulos, BoxLayout.Y_AXIS)
        );

        JLabel titulo = new JLabel("Panel principal");
        titulo.setFont(
                new Font("Segoe UI", Font.BOLD, 25)
        );
        titulo.setForeground(EstiloSIGELAB.AZUL_MARINO);

        JLabel bienvenida = new JLabel(
                "Bienvenido, " + usuario.getNombreCompleto()
                        + ". Resumen general del laboratorio."
        );

        bienvenida.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );
        bienvenida.setForeground(new Color(95, 105, 119));

        titulos.add(titulo);
        titulos.add(Box.createVerticalStrut(5));
        titulos.add(bienvenida);

        encabezado.add(titulos, BorderLayout.CENTER);

        JButton actualizar = new JButton("Actualizar datos");
        actualizar.addActionListener(
                e -> refrescarDashboard(true)
        );

        JPanel acciones = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 4)
        );

        acciones.setOpaque(false);
        acciones.add(actualizar);

        encabezado.add(acciones, BorderLayout.EAST);
        contenido.add(encabezado, BorderLayout.NORTH);

        // Inventario: seis tarjetas
        JPanel panelEquipos = new JPanel(
                new GridLayout(2, 3, 14, 14)
        );
        panelEquipos.setOpaque(false);

        panelEquipos.add(
                crearTarjeta("TOTAL DE EQUIPOS", valorTotal,
                        new Color(42, 91, 145))
        );

        panelEquipos.add(
                crearTarjeta("DISPONIBLES", valorDisponibles,
                        new Color(40, 122, 82))
        );

        panelEquipos.add(
                crearTarjeta("PRESTADOS", valorPrestados,
                        new Color(42, 91, 145))
        );

        panelEquipos.add(
                crearTarjeta("DAÑADOS", valorDanados,
                        new Color(176, 89, 48))
        );

        panelEquipos.add(
                crearTarjeta("EN MANTENIMIENTO", valorMantenimiento,
                        new Color(145, 108, 35))
        );

        panelEquipos.add(
                crearTarjeta("DE BAJA", valorBaja,
                        new Color(111, 119, 132))
        );

        // Préstamos: dos tarjetas
        JPanel panelPrestamos = new JPanel(
                new GridLayout(1, 2, 14, 14)
        );
        panelPrestamos.setOpaque(false);

        panelPrestamos.add(
                crearTarjeta("PRÉSTAMOS ACTIVOS",
                        valorPrestamosActivos,
                        EstiloSIGELAB.AZUL_MARINO)
        );

        panelPrestamos.add(
                crearTarjeta("PRÉSTAMOS VENCIDOS",
                        valorPrestamosVencidos,
                        COLOR_ALERTA)
        );

        JPanel grupos = new JPanel(
                new GridLayout(2, 1, 0, 18)
        );
        grupos.setOpaque(false);

        grupos.add(
                crearGrupo("Inventario de equipos", panelEquipos)
        );

        grupos.add(
                crearGrupo("Control de préstamos", panelPrestamos)
        );

        contenido.add(grupos, BorderLayout.CENTER);

        return contenido;
    }

    private JPanel crearGrupo(
            String titulo,
            JComponent contenidoGrupo) {

        JPanel grupo = new JPanel(
                new BorderLayout(0, 10)
        );
        grupo.setOpaque(false);

        JLabel encabezado = new JLabel(titulo);
        encabezado.setFont(
                new Font("Segoe UI", Font.BOLD, 15)
        );
        encabezado.setForeground(EstiloSIGELAB.TEXTO);

        grupo.add(encabezado, BorderLayout.NORTH);
        grupo.add(contenidoGrupo, BorderLayout.CENTER);

        return grupo;
    }

    // --------------------------------------------------
    // TARJETAS Y NÚMEROS GRANDES
    // --------------------------------------------------

    private JPanel crearTarjeta(
            String titulo,
            JLabel valor,
            Color acento) {

        JPanel tarjeta = new JPanel(
                new BorderLayout(0, 14)
        );

        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0, 5, 0, 0, acento
                        ),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(
                                        EstiloSIGELAB.BORDE
                                ),
                                new EmptyBorder(18, 20, 18, 18)
                        )
                )
        );

        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );
        etiqueta.setForeground(
                new Color(90, 105, 123)
        );

        int tamanoNumero = titulo.startsWith("PRÉSTAMOS")
                ? 48
                : 44;

        valor.setFont(
                new Font("Segoe UI", Font.BOLD, tamanoNumero)
        );
        valor.setHorizontalAlignment(SwingConstants.LEFT);
        valor.setVerticalAlignment(SwingConstants.CENTER);
        valor.setForeground(
                "PRÉSTAMOS VENCIDOS".equals(titulo)
                        && obtenerNumero(valor) > 0
                        ? COLOR_ALERTA
                        : EstiloSIGELAB.AZUL_MARINO
        );

        tarjeta.add(etiqueta, BorderLayout.NORTH);
        tarjeta.add(valor, BorderLayout.CENTER);

        return tarjeta;
    }

    private JLabel crearValor() {
        JLabel valor = new JLabel(
                "—",
                SwingConstants.LEFT
        );

        valor.setFont(
                new Font("Segoe UI", Font.BOLD, 44)
        );
        valor.setForeground(EstiloSIGELAB.AZUL_MARINO);

        return valor;
    }

    /**
     * Reaplica el tamaño de las cifras después del estilo global.
     */
    private void aplicarEstiloNumeros() {
        configurarNumero(valorTotal, 44);
        configurarNumero(valorDisponibles, 44);
        configurarNumero(valorPrestados, 44);
        configurarNumero(valorDanados, 44);
        configurarNumero(valorMantenimiento, 44);
        configurarNumero(valorBaja, 44);
        configurarNumero(valorPrestamosActivos, 48);
        configurarNumero(valorPrestamosVencidos, 48);

        valorPrestamosVencidos.setForeground(
                obtenerNumero(valorPrestamosVencidos) > 0
                        ? COLOR_ALERTA
                        : EstiloSIGELAB.AZUL_MARINO
        );
    }

    private void configurarNumero(JLabel etiqueta, int tamano) {
        etiqueta.setFont(
                new Font("Segoe UI", Font.BOLD, tamano)
        );
        etiqueta.setHorizontalAlignment(SwingConstants.LEFT);
        etiqueta.setVerticalAlignment(SwingConstants.CENTER);
    }

    private int obtenerNumero(JLabel etiqueta) {
        try {
            return Integer.parseInt(etiqueta.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --------------------------------------------------
    // COMPONENTES DEL PANEL LATERAL
    // --------------------------------------------------

    private void agregarSeccion(
            JPanel panel,
            String texto) {

        JLabel etiqueta = new JLabel(texto);

        etiqueta.setForeground(
                new Color(159, 180, 204)
        );
        etiqueta.setFont(
                new Font("Segoe UI", Font.BOLD, 10)
        );
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(etiqueta);
        panel.add(Box.createVerticalStrut(7));
    }

    private void agregarBotonLateral(
            JPanel panel,
            String texto,
            Runnable accion) {

        JButton boton = new JButton(texto);

        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 38)
        );
        boton.setPreferredSize(
                new Dimension(194, 38)
        );

        boton.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );
        boton.setForeground(TEXTO_LATERAL);
        boton.setBackground(AZUL_LATERAL);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        boton.setBorder(
                new EmptyBorder(0, 9, 0, 5)
        );

        boton.addActionListener(e -> accion.run());

        boton.addMouseListener(
                new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(
                            java.awt.event.MouseEvent e) {
                        boton.setBackground(AZUL_HOVER);
                    }

                    @Override
                    public void mouseExited(
                            java.awt.event.MouseEvent e) {
                        boton.setBackground(AZUL_LATERAL);
                    }
                }
        );

        panel.add(boton);
        panel.add(Box.createVerticalStrut(3));
    }

    private Component crearSeparadorLateral() {
        JPanel separador = new JPanel();

        separador.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 1)
        );
        separador.setPreferredSize(
                new Dimension(190, 1)
        );
        separador.setBackground(
                new Color(61, 86, 117)
        );

        return separador;
    }

    private void agregarEspacio(JPanel panel, int alto) {
        panel.add(Box.createVerticalStrut(alto));
    }

    private void aplicarEstiloLateral(Container contenedor) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JButton) {
                JButton boton = (JButton) componente;

                boton.setForeground(TEXTO_LATERAL);
                boton.setBackground(AZUL_LATERAL);
                boton.setOpaque(true);
                boton.setContentAreaFilled(true);
                boton.setBorderPainted(false);
                boton.setFocusPainted(false);
            }

            if (componente instanceof Container) {
                aplicarEstiloLateral((Container) componente);
            }
        }
    }

    // --------------------------------------------------
    // ACTUALIZACIÓN DE ESTADÍSTICAS
    // --------------------------------------------------

    private void enfocarPrincipal() {
        setVisible(true);
        toFront();
    }

    private void refrescarDashboard(boolean mostrarErrores) {
        try {
            DashboardResumen resumen =
                    controladorDashboard.obtener();

            valorTotal.setText(
                    String.valueOf(resumen.getTotalEquipos())
            );

            valorDisponibles.setText(
                    String.valueOf(resumen.getDisponibles())
            );

            valorPrestados.setText(
                    String.valueOf(resumen.getPrestados())
            );

            valorDanados.setText(
                    String.valueOf(resumen.getDanados())
            );

            valorMantenimiento.setText(
                    String.valueOf(resumen.getEnMantenimiento())
            );

            valorBaja.setText(
                    String.valueOf(resumen.getDeBaja())
            );

            valorPrestamosActivos.setText(
                    String.valueOf(resumen.getPrestamosActivos())
            );

            valorPrestamosVencidos.setText(
                    String.valueOf(resumen.getPrestamosVencidos())
            );

            aplicarEstiloNumeros();

        } catch (Exception e) {
            if (mostrarErrores) {
                JOptionPane.showMessageDialog(
                        this,
                        ManejadorErrores.mensajeParaUsuario(e),
                        "No se pudo actualizar el panel",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }

    // --------------------------------------------------
    // APERTURA DE VENTANAS Y CIERRE DE SESIÓN
    // --------------------------------------------------

    private void abrirVentana(
            String clave,
            Supplier<JFrame> fabricaVentana) {

        JFrame ventana = ventanasAbiertas.get(clave);

        if (ventana == null || !ventana.isDisplayable()) {
            ventana = fabricaVentana.get();
            ventanasAbiertas.put(clave, ventana);
        }

        ventana.setVisible(true);
        ventana.toFront();
    }

    private void cerrarSesion() {
        controladorAutenticacion.cerrarSesion();

        for (JFrame ventana : ventanasAbiertas.values()) {
            ventana.dispose();
        }

        new LoginView().setVisible(true);
        dispose();
    }
}
