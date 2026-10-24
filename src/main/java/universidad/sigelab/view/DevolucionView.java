
package universidad.sigelab.view;

import universidad.sigelab.controller.DevolucionController;
import universidad.sigelab.enums.CondicionDevolucion;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.DetallePrestamo;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.PrestamoResumen;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Devoluciones totales o parciales.
 * Las reglas de negocio viven en DevolucionService.
 */
public class DevolucionView extends JFrame {

    // Componentes vinculados con DevolucionView.form
    private JPanel Panelprincipal;
    private JTextField textBuscarEquipo;
    private JButton btBuscarEquipo;
    private JButton btActualizar;
    private JTable tablaPrestamosActivos;
    private JLabel labelPrestamo;
    private JTable tablaPendientes;
    private JComboBox<CondicionDevolucion> comboCondicion;
    private JTextField textObservaciones;
    private JButton btRegistrar;

    // Colores de la interfaz
    private static final Color AZUL_OSCURO =
            new Color(25, 45, 75);

    private static final Color AZUL =
            new Color(41, 98, 160);

    private static final Color VERDE =
            new Color(39, 140, 90);

    private static final Color FONDO =
            new Color(245, 247, 250);

    private static final Color BLANCO =
            Color.WHITE;

    private static final Color NEGRO =
            Color.BLACK;

    private static final Color BORDE =
            new Color(210, 218, 228);

    private static final String[] COLUMNAS_PRESTAMOS = {
            "N.º",
            "Solicitante",
            "Devolución esperada",
            "Equipos",
            "Pendientes",
            "Vencido"
    };

    private static final String[] COLUMNAS_PENDIENTES = {
            "Código",
            "Nombre",
            "Tipo",
            "Laboratorio"
    };

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String TEXTO_SIN_PRESTAMO =
            "Seleccione un préstamo de la lista.";

    private final DevolucionController controladorDevolucion =
            new DevolucionController();

    private final DefaultTableModel modeloPrestamos =
            new DefaultTableModel(COLUMNAS_PRESTAMOS, 0) {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private final DefaultTableModel modeloPendientes =
            new DefaultTableModel(COLUMNAS_PENDIENTES, 0) {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private List<PrestamoResumen> prestamosActivos =
            new ArrayList<>();

    private List<DetallePrestamo> pendientes =
            new ArrayList<>();

    // 0 = ningún préstamo seleccionado.
    private int idPrestamoSeleccionado = 0;

    public DevolucionView() {
        super("SIGELAB | Administración de devoluciones");

        setContentPane(Panelprincipal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarInterfaz();
        configurarComponentes();
        registrarEventos();
        refrescarPrestamos();

        setSize(1060, 680);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
    }

    // =====================================================
    // CONFIGURACIÓN VISUAL
    // =====================================================

    private void configurarInterfaz() {
        if (Panelprincipal != null) {
            Panelprincipal.setBackground(FONDO);
        }

        getContentPane().setBackground(FONDO);

        configurarCampo(textBuscarEquipo);
        configurarCampo(textObservaciones);

        configurarCombo(comboCondicion);

        configurarBoton(btBuscarEquipo, AZUL);
        configurarBoton(btActualizar, AZUL_OSCURO);
        configurarBoton(btRegistrar, VERDE);

        if (labelPrestamo != null) {
            labelPrestamo.setForeground(NEGRO);
            labelPrestamo.setFont(
                    new Font("Segoe UI", Font.BOLD, 13)
            );
        }
    }

    private void configurarCampo(JTextField campo) {
        if (campo == null) {
            return;
        }

        campo.setForeground(NEGRO);
        campo.setCaretColor(NEGRO);
        campo.setBackground(BLANCO);

        campo.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        campo.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        BorderFactory.createEmptyBorder(
                                7, 9, 7, 9
                        )
                )
        );
    }

    private void configurarCombo(
            JComboBox<CondicionDevolucion> combo) {

        if (combo == null) {
            return;
        }

        combo.setForeground(NEGRO);
        combo.setBackground(BLANCO);

        combo.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> lista,
                    Object valor,
                    int indice,
                    boolean seleccionado,
                    boolean tieneFoco) {

                super.getListCellRendererComponent(
                        lista,
                        valor,
                        indice,
                        seleccionado,
                        tieneFoco
                );

                setForeground(NEGRO);

                setBackground(
                        seleccionado
                                ? new Color(220, 232, 246)
                                : BLANCO
                );

                return this;
            }
        });
    }

    /**
     * Aplica un fondo sólido al botón y evita que el tema
     * visual de Swing oculte el color.
     */
    private void configurarBoton(
            JButton boton,
            Color color) {

        if (boton == null) {
            return;
        }

        boton.setUI(new BasicButtonUI());

        boton.setBackground(color);
        boton.setForeground(BLANCO);

        boton.setFont(
                new Font("Segoe UI", Font.BOLD, 13)
        );

        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setRolloverEnabled(false);

        boton.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        boton.setBorder(
                BorderFactory.createEmptyBorder(
                        10, 18, 10, 18
                )
        );

        boton.setMargin(
                new Insets(10, 18, 10, 18)
        );
    }

    // =====================================================
    // CONFIGURACIÓN DE COMPONENTES Y TABLAS
    // =====================================================

    private void configurarComponentes() {
        tablaPrestamosActivos.setModel(modeloPrestamos);

        tablaPrestamosActivos.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        configurarTabla(tablaPrestamosActivos);

        tablaPendientes.setModel(modeloPendientes);

        tablaPendientes.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        );

        configurarTabla(tablaPendientes);

        comboCondicion.removeAllItems();

        comboCondicion.addItem(
                CondicionDevolucion.BUENO
        );

        comboCondicion.addItem(
                CondicionDevolucion.DANADO
        );

        comboCondicion.setSelectedItem(
                CondicionDevolucion.BUENO
        );

        labelPrestamo.setText(TEXTO_SIN_PRESTAMO);
    }

    private void configurarTabla(JTable tabla) {
        tabla.setRowHeight(32);

        tabla.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        tabla.setForeground(NEGRO);
        tabla.setBackground(BLANCO);

        tabla.setSelectionForeground(NEGRO);
        tabla.setSelectionBackground(
                new Color(220, 232, 246)
        );

        tabla.setGridColor(
                new Color(225, 231, 239)
        );

        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);

        tabla.setIntercellSpacing(
                new Dimension(0, 1)
        );

        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);

        // Renderizador de las celdas.
        DefaultTableCellRenderer renderizador =
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable tabla,
                            Object valor,
                            boolean seleccionado,
                            boolean tieneFoco,
                            int fila,
                            int columna) {

                        super.getTableCellRendererComponent(
                                tabla,
                                valor,
                                seleccionado,
                                tieneFoco,
                                fila,
                                columna
                        );

                        setForeground(NEGRO);

                        if (seleccionado) {
                            setBackground(
                                    new Color(220, 232, 246)
                            );
                        } else if (fila % 2 == 0) {
                            setBackground(BLANCO);
                        } else {
                            setBackground(
                                    new Color(246, 248, 251)
                            );
                        }

                        setFont(
                                new Font("Segoe UI", Font.PLAIN, 12)
                        );

                        setBorder(
                                BorderFactory.createEmptyBorder(
                                        0, 10, 0, 10
                                )
                        );

                        return this;
                    }
                };

        for (int columna = 0;
             columna < tabla.getColumnCount();
             columna++) {

            tabla.getColumnModel()
                    .getColumn(columna)
                    .setCellRenderer(renderizador);
        }

        // Encabezado blanco con letras negras.
        JTableHeader encabezado = tabla.getTableHeader();

        encabezado.setForeground(NEGRO);
        encabezado.setBackground(BLANCO);

        encabezado.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        encabezado.setOpaque(true);
        encabezado.setReorderingAllowed(false);

        encabezado.setPreferredSize(
                new Dimension(0, 38)
        );

        encabezado.setDefaultRenderer(
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable tabla,
                            Object valor,
                            boolean seleccionado,
                            boolean tieneFoco,
                            int fila,
                            int columna) {

                        super.getTableCellRendererComponent(
                                tabla,
                                valor,
                                seleccionado,
                                tieneFoco,
                                fila,
                                columna
                        );

                        setForeground(NEGRO);
                        setBackground(BLANCO);

                        setFont(
                                new Font("Segoe UI", Font.BOLD, 12)
                        );

                        setHorizontalAlignment(LEFT);

                        setBorder(
                                BorderFactory.createCompoundBorder(
                                        BorderFactory.createMatteBorder(
                                                0, 0, 1, 1, BORDE
                                        ),
                                        BorderFactory.createEmptyBorder(
                                                0, 10, 0, 10
                                        )
                                )
                        );

                        return this;
                    }
                });
    }

    // =====================================================
    // EVENTOS
    // =====================================================

    private void registrarEventos() {
        tablaPrestamosActivos.getSelectionModel()
                .addListSelectionListener(evento -> {
                    if (!evento.getValueIsAdjusting()) {
                        alSeleccionarPrestamo();
                    }
                });

        btBuscarEquipo.addActionListener(
                evento -> buscarPorEquipo()
        );

        // Permite buscar al presionar Enter,
        // por ejemplo, al utilizar un lector QR.
        textBuscarEquipo.addActionListener(
                evento -> buscarPorEquipo()
        );

        btActualizar.addActionListener(
                evento -> refrescarPrestamos()
        );

        btRegistrar.addActionListener(
                evento -> registrarDevolucion()
        );
    }

    // =====================================================
    // PRÉSTAMOS ACTIVOS
    // =====================================================

    private void refrescarPrestamos() {
        try {
            prestamosActivos = new ArrayList<>(
                    controladorDevolucion.listarPrestamosActivos()
            );

            modeloPrestamos.setRowCount(0);

            for (PrestamoResumen resumen : prestamosActivos) {
                modeloPrestamos.addRow(new Object[]{
                        resumen.getIdPrestamo(),
                        resumen.getSolicitanteNombre(),
                        resumen.getFechaEsperadaDevolucion()
                                .format(FORMATO_FECHA),
                        resumen.getTotalEquipos(),
                        resumen.getEquiposPendientes(),
                        resumen.estaVencido() ? "Sí" : "No"
                });
            }

        } catch (Exception e) {
            mostrarError(e);
        }

        limpiarPendientes();
    }

    private void alSeleccionarPrestamo() {
        int fila = tablaPrestamosActivos.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int filaModelo =
                tablaPrestamosActivos.convertRowIndexToModel(fila);

        if (filaModelo < 0
                || filaModelo >= prestamosActivos.size()) {
            return;
        }

        PrestamoResumen resumen =
                prestamosActivos.get(filaModelo);

        idPrestamoSeleccionado = resumen.getIdPrestamo();

        labelPrestamo.setText(
                "Préstamo N.º "
                        + resumen.getIdPrestamo()
                        + " - "
                        + resumen.getSolicitanteNombre()
                        + ": seleccione los equipos que regresan."
        );

        cargarPendientes();
    }

    /**
     * Selecciona un préstamo por su ID.
     * Devuelve false si no aparece en la lista.
     */
    private boolean seleccionarPrestamo(int idPrestamo) {
        for (int i = 0; i < prestamosActivos.size(); i++) {
            if (prestamosActivos.get(i).getIdPrestamo()
                    == idPrestamo) {

                int filaVista =
                        tablaPrestamosActivos.convertRowIndexToView(i);

                if (filaVista >= 0) {
                    tablaPrestamosActivos.setRowSelectionInterval(
                            filaVista, filaVista
                    );
                    return true;
                }
            }
        }

        return false;
    }

    // =====================================================
    // EQUIPOS PENDIENTES
    // =====================================================

    private void cargarPendientes() {
        try {
            pendientes = new ArrayList<>(
                    controladorDevolucion.listarPendientes(
                            idPrestamoSeleccionado
                    )
            );

            modeloPendientes.setRowCount(0);

            for (DetallePrestamo detalle : pendientes) {
                Equipo equipo = detalle.getEquipo();

                modeloPendientes.addRow(new Object[]{
                        equipo.getCodigoInventario(),
                        equipo.getNombre(),
                        equipo.getTipoEquipo().getNombre(),
                        equipo.getLaboratorio().getCodigo()
                });
            }

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void limpiarPendientes() {
        idPrestamoSeleccionado = 0;
        pendientes = new ArrayList<>();

        modeloPendientes.setRowCount(0);

        labelPrestamo.setText(TEXTO_SIN_PRESTAMO);
    }

    // =====================================================
    // BUSCAR PRÉSTAMO POR CÓDIGO DE EQUIPO
    // =====================================================

    private void buscarPorEquipo() {
        String codigo = textBuscarEquipo.getText().trim();

        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese o escanee el código del equipo.",
                    "Buscar equipo",
                    JOptionPane.INFORMATION_MESSAGE
            );

            textBuscarEquipo.requestFocusInWindow();
            return;
        }

        try {
            int idPrestamo =
                    controladorDevolucion
                            .buscarPrestamoPorCodigoEquipo(codigo);

            refrescarPrestamos();

            if (!seleccionarPrestamo(idPrestamo)) {
                JOptionPane.showMessageDialog(
                        this,
                        "El préstamo N.º " + idPrestamo
                                + " ya no aparece como activo. "
                                + "Actualice la lista.",
                        "Devoluciones",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // =====================================================
    // REGISTRAR DEVOLUCIÓN
    // =====================================================

    private void registrarDevolucion() {
        if (idPrestamoSeleccionado == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    TEXTO_SIN_PRESTAMO,
                    "Devoluciones",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int[] filasSeleccionadas =
                tablaPendientes.getSelectedRows();

        if (filasSeleccionadas.length == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione al menos un equipo para devolver.",
                    "Devoluciones",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        List<Integer> idsDetalle = new ArrayList<>();

        for (int fila : filasSeleccionadas) {
            int filaModelo =
                    tablaPendientes.convertRowIndexToModel(fila);

            if (filaModelo >= 0
                    && filaModelo < pendientes.size()) {

                idsDetalle.add(
                        pendientes.get(filaModelo).getIdDetalle()
                );
            }
        }

        CondicionDevolucion condicion =
                (CondicionDevolucion)
                        comboCondicion.getSelectedItem();

        if (condicion == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione la condición de los equipos.",
                    "Devoluciones",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            int idPrestamo = idPrestamoSeleccionado;

            int restantes =
                    controladorDevolucion.registrarDevolucion(
                            idPrestamo,
                            idsDetalle,
                            condicion,
                            textObservaciones.getText()
                    );

            String mensaje;

            if (restantes == 0) {
                mensaje = "Devolución registrada correctamente. "
                        + "El préstamo N.º " + idPrestamo
                        + " quedó FINALIZADO.";
            } else {
                mensaje = "Devolución registrada correctamente. "
                        + "Quedan " + restantes
                        + " equipo(s) pendiente(s) en el préstamo N.º "
                        + idPrestamo + ".";
            }

            JOptionPane.showMessageDialog(
                    this,
                    mensaje,
                    "Devoluciones",
                    JOptionPane.INFORMATION_MESSAGE
            );

            textObservaciones.setText("");
            textBuscarEquipo.setText("");

            comboCondicion.setSelectedItem(
                    CondicionDevolucion.BUENO
            );

            refrescarPrestamos();

            if (restantes > 0) {
                seleccionarPrestamo(idPrestamo);
            }

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // =====================================================
    // MANEJO DE ERRORES
    // =====================================================

    private void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(
                this,
                ManejadorErrores.mensajeParaUsuario(e),
                "No se pudo completar la operación",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
