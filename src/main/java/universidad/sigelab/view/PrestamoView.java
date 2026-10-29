package universidad.sigelab.view;

import universidad.sigelab.controller.PrestamoController;
import universidad.sigelab.controller.QrController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.PrestamoResumen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Vista de préstamos de SIGELAB.
 *
 * Permite gestionar préstamos con varios equipos y leer
 * códigos QR para identificar equipos.
 *
 * La generación de códigos QR se realiza desde EquiposView.
 * Las reglas de negocio permanecen en los servicios.
 */
public class PrestamoView extends JFrame {

    // Componentes enlazados con PrestamoView.form
    private JPanel Panelprincipal;
    private JTabbedPane tabPrincipal;
    private JTextField textSolicitante;
    private JTextField textIdentificacion;
    private JSpinner spinnerFechaEsperada;
    private JTextField textObservaciones;
    private JTextField textCodigoEquipo;
    private JButton btAgregarEquipo;
    private JButton btQuitarEquipo;
    private JButton btLeerQr;
    private JTable tablaEquiposPrestamo;
    private JButton btLimpiar;
    private JButton btConfirmar;
    private JTable tablaPrestamos;
    private JButton btActualizar;

    // Paleta de colores
    private static final Color AZUL_OSCURO =
            new Color(25, 45, 75);
    private static final Color AZUL =
            new Color(43, 108, 176);
    private static final Color AZUL_HOVER =
            new Color(35, 87, 145);
    private static final Color FONDO =
            new Color(245, 247, 251);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO =
            new Color(45, 55, 72);
    private static final Color GRIS =
            new Color(220, 226, 235);
    private static final Color VERDE =
            new Color(22, 135, 95);
    private static final Color ROJO =
            new Color(194, 55, 55);
    private static final Color GRIS_BOTON =
            new Color(100, 116, 139);

    /*
     * Carpeta predeterminada para buscar los QR generados
     * desde EquiposView.
     *
     * Si tu QrService guarda los códigos en otra carpeta,
     * cambia esta ruta para que coincida con la ubicación real.
     */
    private static final Path CARPETA_QR =
            Path.of(System.getProperty("user.dir"), "codigos_qr");

    private static final String[] COLUMNAS_EQUIPOS = {
            "Código", "Nombre", "Tipo", "Laboratorio", "Estado"
    };

    private static final String[] COLUMNAS_PRESTAMOS = {
            "N.º", "Solicitante", "Fecha del préstamo",
            "Devolución esperada", "Estado", "Equipos",
            "Pendientes", "Vencido"
    };

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PrestamoController controladorPrestamo =
            new PrestamoController();

    private final QrController controladorQr =
            new QrController();

    private final List<Equipo> equiposDelPrestamo =
            new ArrayList<>();

    private final DefaultTableModel modeloEquipos =
            new DefaultTableModel(COLUMNAS_EQUIPOS, 0) {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private final DefaultTableModel modeloPrestamos =
            new DefaultTableModel(COLUMNAS_PRESTAMOS, 0) {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    public PrestamoView() {
        super("SIGELAB | Gestión de préstamos");

        setContentPane(Panelprincipal);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        configurarComponentes();
        aplicarEstiloVisual();
        registrarEventos();
        refrescarPrestamos();

        setSize(1100, 700);
        setMinimumSize(new java.awt.Dimension(900, 600));
        setLocationRelativeTo(null);

        EstiloSIGELAB.aplicar(this);
    }

    // =====================================================
    // CONFIGURACIÓN INICIAL
    // =====================================================

    private void configurarComponentes() {
        spinnerFechaEsperada.setModel(
                new SpinnerDateModel(
                        fechaPorDefecto(),
                        null,
                        null,
                        Calendar.DAY_OF_MONTH
                )
        );

        spinnerFechaEsperada.setEditor(
                new JSpinner.DateEditor(
                        spinnerFechaEsperada,
                        "dd/MM/yyyy HH:mm"
                )
        );

        tablaEquiposPrestamo.setModel(modeloEquipos);
        tablaEquiposPrestamo.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tablaPrestamos.setModel(modeloPrestamos);
    }

    // =====================================================
    // DISEÑO VISUAL
    // =====================================================

    private void aplicarEstiloVisual() {
        getContentPane().setBackground(FONDO);

        if (Panelprincipal != null) {
            Panelprincipal.setBackground(FONDO);
            Panelprincipal.setBorder(
                    new EmptyBorder(16, 18, 16, 18)
            );
        }

        estilizarPestanas();

        estilizarCampo(textSolicitante);
        estilizarCampo(textIdentificacion);
        estilizarCampo(textObservaciones);
        estilizarCampo(textCodigoEquipo);

        estilizarFecha();

        estilizarBoton(btAgregarEquipo, AZUL);
        estilizarBoton(btLeerQr, AZUL_OSCURO);
        estilizarBoton(btQuitarEquipo, ROJO);
        estilizarBoton(btLimpiar, GRIS_BOTON);
        estilizarBoton(btConfirmar, VERDE);
        estilizarBoton(btActualizar, AZUL);

        estilizarTabla(tablaEquiposPrestamo);
        estilizarTabla(tablaPrestamos);

        revalidate();
        repaint();
    }

    private void estilizarPestanas() {
        if (tabPrincipal == null) {
            return;
        }

        tabPrincipal.setFont(
                new Font("Segoe UI", Font.BOLD, 13)
        );
        tabPrincipal.setForeground(TEXTO);
        tabPrincipal.setBackground(BLANCO);
        tabPrincipal.setOpaque(true);
        tabPrincipal.setBorder(
                new EmptyBorder(8, 4, 4, 4)
        );
    }

    private void estilizarCampo(JTextField campo) {
        if (campo == null) {
            return;
        }

        campo.setFont(
                new Font("Segoe UI", Font.PLAIN, 14)
        );
        campo.setForeground(TEXTO);
        campo.setBackground(BLANCO);
        campo.setCaretColor(AZUL);

        campo.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(GRIS, 1, true),
                        new EmptyBorder(7, 10, 7, 10)
                )
        );

        campo.setPreferredSize(
                new java.awt.Dimension(180, 36)
        );
    }

    private void estilizarFecha() {
        if (spinnerFechaEsperada == null) {
            return;
        }

        spinnerFechaEsperada.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );
        spinnerFechaEsperada.setBackground(BLANCO);
        spinnerFechaEsperada.setBorder(
                new LineBorder(GRIS, 1, true)
        );

        JComponent editor = spinnerFechaEsperada.getEditor();

        if (editor instanceof JSpinner.DateEditor) {
            JSpinner.DateEditor dateEditor =
                    (JSpinner.DateEditor) editor;

            dateEditor.getTextField().setFont(
                    new Font("Segoe UI", Font.PLAIN, 13)
            );
            dateEditor.getTextField().setBorder(
                    new EmptyBorder(5, 8, 5, 8)
            );
        }
    }

    private void estilizarBoton(
            JButton boton,
            Color colorOriginal
    ) {
        if (boton == null) {
            return;
        }

        boton.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );
        boton.setForeground(BLANCO);
        boton.setBackground(colorOriginal);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );
        boton.setBorder(
                new EmptyBorder(9, 15, 9, 15)
        );
        boton.setMargin(
                new Insets(5, 10, 5, 10)
        );

        boton.addMouseListener(
                new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(
                            java.awt.event.MouseEvent e
                    ) {
                        if (boton.isEnabled()) {
                            boton.setBackground(
                                    colorOriginal.equals(AZUL)
                                            ? AZUL_HOVER
                                            : colorOriginal.darker()
                            );
                        }
                    }

                    @Override
                    public void mouseExited(
                            java.awt.event.MouseEvent e
                    ) {
                        boton.setBackground(colorOriginal);
                    }
                }
        );
    }

    private void estilizarTabla(JTable tabla) {
        if (tabla == null) {
            return;
        }

        tabla.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );
        tabla.setForeground(TEXTO);
        tabla.setBackground(BLANCO);
        tabla.setGridColor(
                new Color(232, 236, 242)
        );
        tabla.setRowHeight(32);
        tabla.setShowGrid(true);
        tabla.setShowVerticalLines(false);
        tabla.setIntercellSpacing(
                new java.awt.Dimension(0, 1)
        );
        tabla.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        tabla.setSelectionBackground(
                new Color(219, 234, 254)
        );
        tabla.setSelectionForeground(AZUL_OSCURO);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);

        JTableHeaderEstilo(tabla);

        DefaultTableCellRenderer renderizador =
                new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean isSelected,
                            boolean hasFocus,
                            int row,
                            int column
                    ) {
                        Component componente =
                                super.getTableCellRendererComponent(
                                        table,
                                        value,
                                        isSelected,
                                        hasFocus,
                                        row,
                                        column
                                );

                        if (isSelected) {
                            componente.setBackground(
                                    new Color(219, 234, 254)
                            );
                            componente.setForeground(AZUL_OSCURO);
                        } else {
                            componente.setBackground(
                                    row % 2 == 0
                                            ? BLANCO
                                            : new Color(247, 249, 252)
                            );
                            componente.setForeground(TEXTO);
                        }

                        setBorder(
                                new EmptyBorder(4, 9, 4, 9)
                        );

                        return componente;
                    }
                };

        renderizador.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel()
                    .getColumn(i)
                    .setCellRenderer(renderizador);
        }

        if (tabla.getParent() instanceof JViewport) {
            tabla.getParent().setBackground(BLANCO);
        }
    }

    private void JTableHeaderEstilo(JTable tabla) {
        tabla.getTableHeader().setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );
        tabla.getTableHeader().setBackground(AZUL_OSCURO);
        tabla.getTableHeader().setForeground(BLANCO);
        tabla.getTableHeader().setOpaque(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(
                new java.awt.Dimension(0, 38)
        );
    }

    // =====================================================
    // EVENTOS
    // =====================================================

    private void registrarEventos() {
        btAgregarEquipo.addActionListener(
                evento -> agregarEquipo()
        );

        textCodigoEquipo.addActionListener(
                evento -> agregarEquipo()
        );

        btQuitarEquipo.addActionListener(
                evento -> quitarEquipo()
        );

        btLeerQr.addActionListener(
                evento -> leerQr()
        );

        btLimpiar.addActionListener(
                evento -> limpiarFormulario()
        );

        btConfirmar.addActionListener(
                evento -> confirmarPrestamo()
        );

        btActualizar.addActionListener(
                evento -> refrescarPrestamos()
        );

        tabPrincipal.addChangeListener(evento -> {
            if (tabPrincipal.getSelectedIndex() == 1) {
                refrescarPrestamos();
            }
        });
    }

    // =====================================================
    // AGREGAR EQUIPOS
    // =====================================================

    private void agregarEquipo() {
        try {
            Equipo equipo =
                    controladorPrestamo.identificarEquipo(
                            textCodigoEquipo.getText(),
                            equiposDelPrestamo
                    );

            equiposDelPrestamo.add(equipo);

            llenarTablaEquipos();

            textCodigoEquipo.setText("");
            textCodigoEquipo.requestFocusInWindow();

        } catch (Exception e) {
            mostrarError(e);
            textCodigoEquipo.selectAll();
            textCodigoEquipo.requestFocusInWindow();
        }
    }

    // =====================================================
    // LEER CÓDIGO QR DESDE UNA IMAGEN
    // =====================================================

    private void leerQr() {
        File carpetaInicial = CARPETA_QR.toFile();

        JFileChooser selector = new JFileChooser(
                carpetaInicial.isDirectory()
                        ? carpetaInicial
                        : new File(System.getProperty("user.dir"))
        );

        selector.setDialogTitle(
                "Seleccione la imagen del código QR del equipo"
        );

        selector.setFileFilter(
                new FileNameExtensionFilter(
                        "Imágenes (PNG, JPG, JPEG, GIF, BMP)",
                        "png", "jpg", "jpeg", "gif", "bmp"
                )
        );

        if (selector.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivoSeleccionado = selector.getSelectedFile();

        try {
            String codigo = controladorQr.leerCodigo(
                    archivoSeleccionado
            );

            if (codigo == null || codigo.trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se encontró un código válido en la imagen.",
                        "Lectura de QR",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            textCodigoEquipo.setText(codigo.trim());

            /*
             * El QR solo identifica el equipo.
             * agregarEquipo() comprueba que sea válido y que
             * no esté agregado previamente, según el controlador.
             * No confirma el préstamo automáticamente.
             */
            agregarEquipo();

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // =====================================================
    // QUITAR EQUIPO
    // =====================================================

    private void quitarEquipo() {
        int fila = tablaEquiposPrestamo.getSelectedRow();

        if (fila < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione un equipo de la lista.",
                    "Préstamos",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int filaModelo =
                tablaEquiposPrestamo.convertRowIndexToModel(fila);

        equiposDelPrestamo.remove(filaModelo);

        llenarTablaEquipos();
    }

    // =====================================================
    // CONFIRMAR PRÉSTAMO
    // =====================================================

    private void confirmarPrestamo() {
        try {
            int cantidadEquipos = equiposDelPrestamo.size();

            int idPrestamo =
                    controladorPrestamo.confirmarPrestamo(
                            textSolicitante.getText(),
                            textIdentificacion.getText(),
                            fechaEsperada(),
                            textObservaciones.getText(),
                            equiposDelPrestamo
                    );

            JOptionPane.showMessageDialog(
                    this,
                    "Préstamo N.º " + idPrestamo
                            + " registrado con "
                            + cantidadEquipos
                            + " equipo(s).",
                    "Préstamo registrado",
                    JOptionPane.INFORMATION_MESSAGE
            );

            limpiarFormulario();
            refrescarPrestamos();

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // =====================================================
    // LIMPIAR FORMULARIO
    // =====================================================

    private void limpiarFormulario() {
        textSolicitante.setText("");
        textIdentificacion.setText("");

        spinnerFechaEsperada.setValue(
                fechaPorDefecto()
        );

        textObservaciones.setText("");
        textCodigoEquipo.setText("");

        equiposDelPrestamo.clear();

        llenarTablaEquipos();

        textSolicitante.requestFocusInWindow();
    }

    // =====================================================
    // TABLA DE EQUIPOS
    // =====================================================

    private void llenarTablaEquipos() {
        modeloEquipos.setRowCount(0);

        for (Equipo equipo : equiposDelPrestamo) {
            modeloEquipos.addRow(new Object[]{
                    equipo.getCodigoInventario(),
                    equipo.getNombre(),
                    equipo.getTipoEquipo().getNombre(),
                    equipo.getLaboratorio().getCodigo(),
                    equipo.getEstado().name()
            });
        }
    }

    // =====================================================
    // TABLA DE PRÉSTAMOS
    // =====================================================

    private void refrescarPrestamos() {
        try {
            modeloPrestamos.setRowCount(0);

            for (PrestamoResumen resumen
                    : controladorPrestamo.listarResumen()) {

                modeloPrestamos.addRow(new Object[]{
                        resumen.getIdPrestamo(),
                        resumen.getSolicitanteNombre(),
                        resumen.getFechaPrestamo()
                                .format(FORMATO_FECHA),
                        resumen.getFechaEsperadaDevolucion()
                                .format(FORMATO_FECHA),
                        resumen.getEstado().name(),
                        resumen.getTotalEquipos(),
                        resumen.getEquiposPendientes(),
                        resumen.estaVencido() ? "Sí" : "No"
                });
            }

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // =====================================================
    // UTILIDADES
    // =====================================================

    private static Date fechaPorDefecto() {
        return Date.from(
                LocalDateTime.now()
                        .plusDays(1)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );
    }

    private LocalDateTime fechaEsperada() {
        Date fecha =
                (Date) spinnerFechaEsperada.getValue();

        return LocalDateTime.ofInstant(
                fecha.toInstant(),
                ZoneId.systemDefault()
        );
    }

    private void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(
                this,
                ManejadorErrores.mensajeParaUsuario(e),
                "No se pudo completar la operación",
                JOptionPane.WARNING_MESSAGE
        );
    }
}

