
package universidad.sigelab.view;

import universidad.sigelab.controller.TipoEquipoController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del catálogo de tipos de equipo.
 * Las reglas de negocio viven en TipoEquipoService.
 */
public class TipoEquipoView extends JFrame {

    // Componentes vinculados con TipoEquipoView.form
    private JPanel Panelprincipal;
    private JTable tablaTipos;
    private JTextField textNombre;
    private JTextField textDescripcion;
    private JButton btNuevo;
    private JButton btGuardar;

    // Colores de la interfaz
    private static final Color AZUL_OSCURO =
            new Color(25, 45, 75);

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

    private static final String[] COLUMNAS = {
            "Nombre",
            "Descripción"
    };

    private final TipoEquipoController controladorTipoEquipo =
            new TipoEquipoController();

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(COLUMNAS, 0) {

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private List<TipoEquipo> tiposMostrados =
            new ArrayList<>();

    // 0 = nuevo tipo; otro valor = tipo seleccionado.
    private int idTipoSeleccionado = 0;

    public TipoEquipoView() {
        super("SIGELAB | Tipos de equipo");

        setContentPane(Panelprincipal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarInterfaz();
        configurarTabla();
        registrarEventos();
        refrescarTabla();

        setSize(850, 560);
        setMinimumSize(new Dimension(700, 480));
        setLocationRelativeTo(null);
        EstiloSIGELAB.aplicar(this);
    }

    // =====================================================
    // CONFIGURACIÓN VISUAL
    // =====================================================

    private void configurarInterfaz() {
        if (Panelprincipal != null) {
            Panelprincipal.setBackground(FONDO);
        }

        getContentPane().setBackground(FONDO);

        configurarCampo(textNombre);
        configurarCampo(textDescripcion);

        configurarBoton(btNuevo, AZUL_OSCURO);
        configurarBoton(btGuardar, VERDE);
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

    /**
     * Aplica un fondo sólido al botón, con texto blanco.
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
    // CONFIGURACIÓN DE LA TABLA
    // =====================================================

    private void configurarTabla() {
        tablaTipos.setModel(modeloTabla);

        tablaTipos.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tablaTipos.setRowHeight(34);

        tablaTipos.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        tablaTipos.setForeground(NEGRO);
        tablaTipos.setBackground(BLANCO);

        tablaTipos.setSelectionForeground(NEGRO);
        tablaTipos.setSelectionBackground(
                new Color(220, 232, 246)
        );

        tablaTipos.setGridColor(
                new Color(225, 231, 239)
        );

        tablaTipos.setShowHorizontalLines(true);
        tablaTipos.setShowVerticalLines(false);

        tablaTipos.setIntercellSpacing(
                new Dimension(0, 1)
        );

        tablaTipos.setFillsViewportHeight(true);
        tablaTipos.setAutoCreateRowSorter(true);

        // Texto negro y filas alternadas.
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
             columna < tablaTipos.getColumnCount();
             columna++) {

            tablaTipos.getColumnModel()
                    .getColumn(columna)
                    .setCellRenderer(renderizador);
        }

        // Encabezado blanco con texto negro.
        JTableHeader encabezado =
                tablaTipos.getTableHeader();

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

        tablaTipos.getSelectionModel()
                .addListSelectionListener(evento -> {
                    if (!evento.getValueIsAdjusting()) {
                        alSeleccionarFila();
                    }
                });
    }

    // =====================================================
    // EVENTOS
    // =====================================================

    private void registrarEventos() {
        btNuevo.addActionListener(
                evento -> limpiarFormulario()
        );

        btGuardar.addActionListener(
                evento -> guardar()
        );
    }

    // =====================================================
    // CARGAR TIPOS DE EQUIPO
    // =====================================================

    private void refrescarTabla() {
        try {
            tiposMostrados = new ArrayList<>(
                    controladorTipoEquipo.listar()
            );

            modeloTabla.setRowCount(0);

            for (TipoEquipo tipo : tiposMostrados) {
                modeloTabla.addRow(new Object[]{
                        tipo.getNombre(),
                        tipo.getDescripcion()
                });
            }

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void alSeleccionarFila() {
        int fila = tablaTipos.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int filaModelo =
                tablaTipos.convertRowIndexToModel(fila);

        if (filaModelo < 0
                || filaModelo >= tiposMostrados.size()) {
            return;
        }

        TipoEquipo tipo = tiposMostrados.get(filaModelo);

        idTipoSeleccionado = tipo.getIdTipoEquipo();

        textNombre.setText(tipo.getNombre());
        textDescripcion.setText(tipo.getDescripcion());
    }

    // =====================================================
    // LIMPIAR FORMULARIO
    // =====================================================

    private void limpiarFormulario() {
        idTipoSeleccionado = 0;

        tablaTipos.clearSelection();

        textNombre.setText("");
        textDescripcion.setText("");

        textNombre.requestFocusInWindow();
    }

    // =====================================================
    // REGISTRAR Y MODIFICAR TIPOS
    // =====================================================

    private void guardar() {
        try {
            TipoEquipo guardado;

            if (idTipoSeleccionado == 0) {
                guardado = controladorTipoEquipo.registrar(
                        textNombre.getText(),
                        textDescripcion.getText()
                );

            } else {
                guardado = controladorTipoEquipo.modificar(
                        idTipoSeleccionado,
                        textNombre.getText(),
                        textDescripcion.getText()
                );
            }

            JOptionPane.showMessageDialog(
                    this,
                    "El tipo de equipo "
                            + guardado.getNombre()
                            + " se guardó correctamente.",
                    "Tipos de equipo",
                    JOptionPane.INFORMATION_MESSAGE
            );

            limpiarFormulario();
            refrescarTabla();

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
