package universidad.sigelab.view;

import universidad.sigelab.controller.AutenticacionController;
import universidad.sigelab.controller.DashboardController;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.DashboardResumen;
import universidad.sigelab.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PrincipalFrame extends JFrame {

    private static final Color COLOR_ALERTA = new Color(0xB00020);

    private final Usuario usuario;
    private final AutenticacionController controladorAutenticacion = new AutenticacionController();
    private final DashboardController controladorDashboard = new DashboardController();
    private final Map<String, JFrame> ventanasAbiertas = new HashMap<>();

    // Contadores del dashboard (sección 6.11 de la guía)
    private final JLabel valorTotal = crearValor();
    private final JLabel valorDisponibles = crearValor();
    private final JLabel valorPrestados = crearValor();
    private final JLabel valorDanados = crearValor();
    private final JLabel valorMantenimiento = crearValor();
    private final JLabel valorBaja = crearValor();
    private final JLabel valorPrestamosActivos = crearValor();
    private final JLabel valorPrestamosVencidos = crearValor();

    public PrincipalFrame(Usuario usuario) {
        super("SIGELAB - Panel principal");
        this.usuario = usuario;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        construirMenu();
        construirContenido();
        refrescarDashboard(false);

        // Al volver a esta ventana (por ejemplo, después de cerrar Préstamos) se recalculan las cantidades.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent evento) {
                refrescarDashboard(false);
            }
        });

        setSize(900, 560);
        setLocationRelativeTo(null);
    }

    // ---------- Menú ----------


    private void construirMenu() {
        RolUsuario rol = usuario.getRol().getNombre();
        boolean esAdmin = rol == RolUsuario.ADMIN;
        boolean puedeOperar = esAdmin || rol == RolUsuario.ENCARGADO;

        JMenuBar barraMenu = new JMenuBar();

        // ----- Catálogos -----
        JMenu menuCatalogos = new JMenu("Catálogos");

        agregarOpcion(
                menuCatalogos,
                "Equipos",
                "equipos",
                () -> new EquiposView(usuario)
        );

        if (esAdmin) {
            agregarOpcion(
                    menuCatalogos,
                    "Laboratorios",
                    "laboratorios",
                    LaboratorioView::new
            );

            agregarOpcion(
                    menuCatalogos,
                    "Tipos de equipo",
                    "tipos",
                    TipoEquipoView::new
            );

            agregarOpcion(
                    menuCatalogos,
                    "Usuarios",
                    "usuarios",
                    UsuarioView::new
            );
        }

        barraMenu.add(menuCatalogos);

        // ----- Operaciones -----
        if (puedeOperar) {
            JMenu menuOperaciones = new JMenu("Operaciones");

            agregarOpcion(
                    menuOperaciones,
                    "Préstamos",
                    "prestamos",
                    PrestamoView::new
            );

            agregarOpcion(
                    menuOperaciones,
                    "Devoluciones",
                    "devoluciones",
                    DevolucionView::new
            );

            barraMenu.add(menuOperaciones);
        }

        // ----- Reportes -----
        if (puedeOperar) {
            JMenu menuReportes = new JMenu("Reportes");

            agregarOpcion(
                    menuReportes,
                    "Consultas y reportes",
                    "reportes",
                    ReporteFrame::new
            );

            if (esAdmin) {
                agregarOpcion(
                        menuReportes,
                        "Auditoría",
                        "auditoria",
                        AuditoriaView::new
                );
            }

            barraMenu.add(menuReportes);
        }

        // ----- Sesión -----
        JMenu menuSesion = new JMenu("Sesión");

        JMenuItem itemCerrar = new JMenuItem("Cerrar sesión");
        itemCerrar.addActionListener(evento -> cerrarSesion());

        menuSesion.add(itemCerrar);
        barraMenu.add(menuSesion);

        setJMenuBar(barraMenu);
    }


    private void agregarOpcion(JMenu menu, String texto, String clave, Supplier<JFrame> fabricaVentana) {
        JMenuItem opcion = new JMenuItem(texto);
        opcion.addActionListener(evento -> abrirVentana(clave, fabricaVentana));
        menu.add(opcion);
    }

    // ---------- Contenido: bienvenida y dashboard ----------

    private void construirContenido() {
        JLabel bienvenida = new JLabel(
                "Bienvenido, " + usuario.getNombreCompleto() + " (" + usuario.getRol() + ")",
                SwingConstants.LEFT);
        bienvenida.setFont(bienvenida.getFont().deriveFont(Font.BOLD, 18f));

        JButton botonActualizar = new JButton("Actualizar");
        botonActualizar.addActionListener(evento -> refrescarDashboard(true));
        JPanel panelEncabezado = new JPanel(new BorderLayout());
        panelEncabezado.add(bienvenida, BorderLayout.WEST);
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelBoton.add(botonActualizar);
        panelEncabezado.add(panelBoton, BorderLayout.EAST);

        JPanel panelEquipos = new JPanel(new GridLayout(1, 0, 12, 12));
        panelEquipos.setBorder(BorderFactory.createTitledBorder("Equipos"));
        panelEquipos.add(crearTarjeta("Total", valorTotal));
        panelEquipos.add(crearTarjeta("Disponibles", valorDisponibles));
        panelEquipos.add(crearTarjeta("Prestados", valorPrestados));
        panelEquipos.add(crearTarjeta("Dañados", valorDanados));
        panelEquipos.add(crearTarjeta("En mantenimiento", valorMantenimiento));
        panelEquipos.add(crearTarjeta("De baja", valorBaja));

        JPanel panelPrestamos = new JPanel(new GridLayout(1, 0, 12, 12));
        panelPrestamos.setBorder(BorderFactory.createTitledBorder("Préstamos"));
        panelPrestamos.add(crearTarjeta("Activos", valorPrestamosActivos));
        panelPrestamos.add(crearTarjeta("Vencidos", valorPrestamosVencidos));

        JPanel panelCentro = new JPanel(new GridLayout(0, 1, 0, 16));
        panelCentro.add(panelEquipos);
        panelCentro.add(panelPrestamos);

        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        panel.add(panelEncabezado, BorderLayout.NORTH);
        panel.add(panelCentro, BorderLayout.CENTER);
        setContentPane(panel);
    }

    private JLabel crearValor() {
        JLabel valor = new JLabel("-", SwingConstants.CENTER);
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 32f));
        return valor;
    }

    private JPanel crearTarjeta(String titulo, JLabel valor) {
        JLabel etiquetaTitulo = new JLabel(titulo, SwingConstants.CENTER);
        JPanel tarjeta = new JPanel(new BorderLayout(0, 6));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(14, 8, 14, 8)));
        tarjeta.add(valor, BorderLayout.CENTER);
        tarjeta.add(etiquetaTitulo, BorderLayout.SOUTH);
        return tarjeta;
    }

    /** Las cantidades salen de la base de datos (vista vw_dashboard), no de variables en pantalla. */
    private void refrescarDashboard(boolean mostrarErrores) {
        try {
            DashboardResumen resumen = controladorDashboard.obtener();
            valorTotal.setText(String.valueOf(resumen.getTotalEquipos()));
            valorDisponibles.setText(String.valueOf(resumen.getDisponibles()));
            valorPrestados.setText(String.valueOf(resumen.getPrestados()));
            valorDanados.setText(String.valueOf(resumen.getDanados()));
            valorMantenimiento.setText(String.valueOf(resumen.getEnMantenimiento()));
            valorBaja.setText(String.valueOf(resumen.getDeBaja()));
            valorPrestamosActivos.setText(String.valueOf(resumen.getPrestamosActivos()));
            valorPrestamosVencidos.setText(String.valueOf(resumen.getPrestamosVencidos()));
            valorPrestamosVencidos.setForeground(resumen.getPrestamosVencidos() > 0
                    ? COLOR_ALERTA : valorPrestamosActivos.getForeground());
        } catch (Exception e) {
            if (mostrarErrores) {
                JOptionPane.showMessageDialog(this, ManejadorErrores.mensajeParaUsuario(e),
                        "No se pudo actualizar el panel", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ---------- Ventanas y sesión ----------

    /** Reutiliza la ventana si ya está abierta, para no duplicarla. */
    private void abrirVentana(String clave, Supplier<JFrame> fabricaVentana) {
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
