
package universidad.sigelab.view;

import universidad.sigelab.controller.ReporteController;
import universidad.sigelab.enums.FiltroPrestamos;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.ResultadoReporte;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * Consultas y reportes de SIGELAB.
 * Solo captura parámetros y muestra resultados.
 * Las consultas SQL permanecen en la capa Repository.
 */
public class ReporteFrame extends JFrame {

    private static final String[] REPORTES = {
            "Equipos por estado",
            "Equipos por laboratorio y estado",
            "Préstamos (activos, finalizados, vencidos)",
            "Préstamos entre fechas",
            "Historial de préstamos de un equipo",
            "Historial de mantenimiento de un equipo"
    };

    private static final int EQUIPOS_POR_ESTADO = 0;
    private static final int EQUIPOS_POR_LABORATORIO = 1;
    private static final int PRESTAMOS_POR_ESTADO = 2;
    private static final int PRESTAMOS_ENTRE_FECHAS = 3;
    private static final int HISTORIAL_PRESTAMOS = 4;
    private static final int HISTORIAL_MANTENIMIENTO = 5;

    // Paleta institucional
    private static final Color AZUL_MARINO = new Color(23, 54, 93);
    private static final Color AZUL_SELECCION = new Color(226, 236, 248);
    private static final Color FONDO = new Color(245, 247, 250);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(35, 42, 52);
    private static final Color BORDE = new Color(218, 224, 232);

    private final ReporteController controladorReporte =
            new ReporteController();

    private final JComboBox<String> comboReporte =
            new JComboBox<>(REPORTES);

    private final JComboBox<FiltroPrestamos> comboFiltroPrestamos =
            new JComboBox<>(FiltroPrestamos.values());

    private final JTextField campoCodigoEquipo = new JTextField(14);

    private final JSpinner spinnerDesde = new JSpinner(
            new SpinnerDateModel(
                    fechaHaceDias(30),
                    null,
                    null,
                    Calendar.DAY_OF_MONTH
            )
    );

    private final JSpinner spinnerHasta = new JSpinner(
            new SpinnerDateModel(
                    new Date(),
                    null,
                    null,
                    Calendar.DAY_OF_MONTH
            )
    );

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private final JTable tabla = new JTable(modeloTabla);

    private final JLabel etiquetaTotal =
            new JLabel("Seleccione un reporte y pulse Consultar.");

    private final JButton botonConsultar = new JButton("Consultar");

    public ReporteFrame() {
        super("SIGELAB - Consultas y reportes");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        construirInterfaz();
        actualizarParametros();

        setMinimumSize(new Dimension(1000, 600));
        setSize(1200, 650);
        setLocationRelativeTo(null);

        EstiloSIGELAB.aplicar(this);
    }

    private void construirInterfaz() {

        // Configuración de fechas
        spinnerDesde.setEditor(
                new JSpinner.DateEditor(spinnerDesde, "dd/MM/yyyy")
        );

        spinnerHasta.setEditor(
                new JSpinner.DateEditor(spinnerHasta, "dd/MM/yyyy")
        );

        // Eventos
        comboReporte.addActionListener(
                evento -> actualizarParametros()
        );

        campoCodigoEquipo.addActionListener(
                evento -> consultar()
        );

        botonConsultar.addActionListener(
                evento -> consultar()
        );

        // Configurar componentes
        configurarComponentes();
        configurarTabla();

        // Panel de filtros
        JPanel panelFiltros = new JPanel(new GridBagLayout());
        panelFiltros.setBackground(BLANCO);
        panelFiltros.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                )
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tipo de reporte
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        panelFiltros.add(comboReporte, gbc);

        // Filtro de préstamos
        gbc.gridx = 1;
        gbc.weightx = 0;
        panelFiltros.add(new JLabel("Préstamos:"), gbc);

        gbc.gridx = 2;
        panelFiltros.add(comboFiltroPrestamos, gbc);

        // Fecha inicial
        gbc.gridx = 3;
        panelFiltros.add(new JLabel("Desde:"), gbc);

        gbc.gridx = 4;
        panelFiltros.add(spinnerDesde, gbc);

        // Fecha final
        gbc.gridx = 5;
        panelFiltros.add(new JLabel("Hasta:"), gbc);

        gbc.gridx = 6;
        panelFiltros.add(spinnerHasta, gbc);

        // Código del equipo
        gbc.gridx = 7;
        panelFiltros.add(new JLabel("Código del equipo:"), gbc);

        gbc.gridx = 8;
        gbc.weightx = 0.5;
        panelFiltros.add(campoCodigoEquipo, gbc);

        // Botón en un panel independiente
        JPanel panelAcciones = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 4)
        );
        panelAcciones.setBackground(FONDO);
        panelAcciones.add(botonConsultar);

        // Zona superior: filtros y botón separados
        JPanel panelSuperior = new JPanel(new BorderLayout(0, 8));
        panelSuperior.setBackground(FONDO);
        panelSuperior.add(panelFiltros, BorderLayout.CENTER);
        panelSuperior.add(panelAcciones, BorderLayout.SOUTH);

        // Panel de resultados
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(
                BorderFactory.createLineBorder(BORDE)
        );
        scrollTabla.getViewport().setBackground(BLANCO);

        // Etiqueta de registros encontrados
        etiquetaTotal.setForeground(TEXTO);
        etiquetaTotal.setFont(
                new Font("SansSerif", Font.PLAIN, 12)
        );
        etiquetaTotal.setBorder(
                BorderFactory.createEmptyBorder(8, 4, 2, 4)
        );

        // Contenedor principal
        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setBackground(FONDO);
        contenido.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        contenido.add(panelSuperior, BorderLayout.NORTH);
        contenido.add(scrollTabla, BorderLayout.CENTER);
        contenido.add(etiquetaTotal, BorderLayout.SOUTH);

        setContentPane(contenido);
    }

    private void configurarComponentes() {

        // Fondo y texto de los filtros
        comboReporte.setBackground(BLANCO);
        comboReporte.setForeground(TEXTO);
        comboReporte.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );
        comboReporte.setPreferredSize(new Dimension(300, 34));

        comboFiltroPrestamos.setBackground(BLANCO);
        comboFiltroPrestamos.setForeground(TEXTO);
        comboFiltroPrestamos.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );
        comboFiltroPrestamos.setPreferredSize(
                new Dimension(135, 34)
        );

        campoCodigoEquipo.setBackground(BLANCO);
        campoCodigoEquipo.setForeground(TEXTO);
        campoCodigoEquipo.setCaretColor(AZUL_MARINO);
        campoCodigoEquipo.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );
        campoCodigoEquipo.setPreferredSize(
                new Dimension(145, 34)
        );
        campoCodigoEquipo.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        BorderFactory.createEmptyBorder(4, 7, 4, 7)
                )
        );

        configurarSpinner(spinnerDesde);
        configurarSpinner(spinnerHasta);

        // Botón principal
        botonConsultar.setFont(
                new Font("SansSerif", Font.BOLD, 13)
        );
        botonConsultar.setForeground(BLANCO);
        botonConsultar.setBackground(AZUL_MARINO);
        botonConsultar.setOpaque(true);
        botonConsultar.setContentAreaFilled(true);
        botonConsultar.setBorderPainted(false);
        botonConsultar.setFocusPainted(false);
        botonConsultar.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        botonConsultar.setPreferredSize(
                new Dimension(135, 38)
        );
        botonConsultar.setMinimumSize(
                new Dimension(135, 38)
        );
        botonConsultar.setToolTipText(
                "Generar el reporte seleccionado"
        );

        // Etiquetas de los filtros
        Font fuenteEtiqueta =
                new Font("SansSerif", Font.PLAIN, 12);

        Color colorEtiqueta = new Color(55, 65, 81);

        for (java.awt.Component componente :
                obtenerEtiquetas()) {
            componente.setFont(fuenteEtiqueta);
            componente.setForeground(colorEtiqueta);
        }
    }

    private java.awt.Component[] obtenerEtiquetas() {
        return new java.awt.Component[]{
                new JLabel(""),
        };
    }

    private void configurarSpinner(JSpinner spinner) {
        spinner.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );
        spinner.setPreferredSize(new Dimension(125, 34));
        spinner.setBorder(
                BorderFactory.createLineBorder(BORDE)
        );

        JSpinner.DefaultEditor editor =
                (JSpinner.DefaultEditor) spinner.getEditor();

        editor.getTextField().setBackground(BLANCO);
        editor.getTextField().setForeground(TEXTO);
        editor.getTextField().setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );
        editor.getTextField().setHorizontalAlignment(
                SwingConstants.CENTER
        );
    }

    private void configurarTabla() {

        tabla.setBackground(BLANCO);
        tabla.setForeground(TEXTO);
        tabla.setFont(
                new Font("SansSerif", Font.PLAIN, 12)
        );
        tabla.setRowHeight(28);
        tabla.setFillsViewportHeight(true);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(232, 236, 242));
        tabla.setSelectionBackground(AZUL_SELECCION);
        tabla.setSelectionForeground(TEXTO);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setAutoCreateRowSorter(true);

        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setBackground(AZUL_MARINO);
        encabezado.setForeground(BLANCO);
        encabezado.setFont(
                new Font("SansSerif", Font.BOLD, 12)
        );
        encabezado.setPreferredSize(new Dimension(0, 36));
        encabezado.setReorderingAllowed(false);

        // Alternar el color de las filas sin perder la selección
        DefaultTableCellRenderer renderizador =
                new DefaultTableCellRenderer() {

                    @Override
                    public java.awt.Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean isSelected,
                            boolean hasFocus,
                            int row,
                            int column) {

                        java.awt.Component componente =
                                super.getTableCellRendererComponent(
                                        table,
                                        value,
                                        isSelected,
                                        hasFocus,
                                        row,
                                        column
                                );

                        if (isSelected) {
                            componente.setBackground(AZUL_SELECCION);
                            componente.setForeground(TEXTO);
                        } else {
                            componente.setBackground(
                                    row % 2 == 0
                                            ? BLANCO
                                            : new Color(247, 249, 252)
                            );
                            componente.setForeground(TEXTO);
                        }

                        setBorder(
                                BorderFactory.createEmptyBorder(
                                        0, 8, 0, 8
                                )
                        );

                        return componente;
                    }
                };

        tabla.setDefaultRenderer(Object.class, renderizador);
    }

    /**
     * Solo se habilitan los parámetros que necesita cada reporte.
     */
    private void actualizarParametros() {

        int reporte = comboReporte.getSelectedIndex();

        comboFiltroPrestamos.setEnabled(
                reporte == PRESTAMOS_POR_ESTADO
        );

        spinnerDesde.setEnabled(
                reporte == PRESTAMOS_ENTRE_FECHAS
        );

        spinnerHasta.setEnabled(
                reporte == PRESTAMOS_ENTRE_FECHAS
        );

        campoCodigoEquipo.setEnabled(
                reporte == HISTORIAL_PRESTAMOS
                        || reporte == HISTORIAL_MANTENIMIENTO
        );

        // Atenuar visualmente los campos no utilizados
        comboFiltroPrestamos.setBackground(
                comboFiltroPrestamos.isEnabled()
                        ? BLANCO : new Color(240, 242, 245)
        );

        spinnerDesde.setBackground(
                spinnerDesde.isEnabled()
                        ? BLANCO : new Color(240, 242, 245)
        );

        spinnerHasta.setBackground(
                spinnerHasta.isEnabled()
                        ? BLANCO : new Color(240, 242, 245)
        );

        campoCodigoEquipo.setBackground(
                campoCodigoEquipo.isEnabled()
                        ? BLANCO : new Color(240, 242, 245)
        );
    }

    private void consultar() {

        try {
            ResultadoReporte resultado;

            switch (comboReporte.getSelectedIndex()) {

                case EQUIPOS_POR_ESTADO:
                    resultado =
                            controladorReporte.equiposPorEstado();
                    break;

                case EQUIPOS_POR_LABORATORIO:
                    resultado =
                            controladorReporte.equiposPorLaboratorioYEstado();
                    break;

                case PRESTAMOS_POR_ESTADO:
                    resultado = controladorReporte.prestamos(
                            (FiltroPrestamos)
                                    comboFiltroPrestamos.getSelectedItem()
                    );
                    break;

                case PRESTAMOS_ENTRE_FECHAS:
                    LocalDate desde = aFecha(spinnerDesde);
                    LocalDate hasta = aFecha(spinnerHasta);

                    if (desde.isAfter(hasta)) {
                        JOptionPane.showMessageDialog(
                                this,
                                "La fecha inicial no puede ser posterior a la fecha final.",
                                "Fechas incorrectas",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }

                    resultado =
                            controladorReporte.prestamosEntreFechas(
                                    desde,
                                    hasta
                            );
                    break;

                case HISTORIAL_PRESTAMOS:
                    if (campoCodigoEquipo.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Ingrese el código del equipo.",
                                "Dato requerido",
                                JOptionPane.WARNING_MESSAGE
                        );
                        campoCodigoEquipo.requestFocusInWindow();
                        return;
                    }

                    resultado =
                            controladorReporte.historialPrestamosDeEquipo(
                                    campoCodigoEquipo.getText().trim()
                            );
                    break;

                case HISTORIAL_MANTENIMIENTO:
                    if (campoCodigoEquipo.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Ingrese el código del equipo.",
                                "Dato requerido",
                                JOptionPane.WARNING_MESSAGE
                        );
                        campoCodigoEquipo.requestFocusInWindow();
                        return;
                    }

                    resultado =
                            controladorReporte.historialMantenimientoDeEquipo(
                                    campoCodigoEquipo.getText().trim()
                            );
                    break;

                default:
                    return;
            }

            mostrar(resultado);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    ManejadorErrores.mensajeParaUsuario(e),
                    "No se pudo consultar el reporte",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void mostrar(ResultadoReporte resultado) {

        Object[][] filas =
                resultado.getFilas().toArray(new Object[0][]);

        modeloTabla.setDataVector(
                filas,
                resultado.getColumnas().toArray()
        );

        // Mantener el estilo de la tabla después de cambiar columnas
        configurarTabla();

        etiquetaTotal.setText(
                filas.length + " registro(s) encontrado(s)."
        );
    }

    // ---------- Utilidades ----------

    private static Date fechaHaceDias(int dias) {
        return Date.from(
                LocalDate.now()
                        .minusDays(dias)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );
    }

    private LocalDate aFecha(JSpinner spinner) {
        return ((Date) spinner.getValue())
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}
