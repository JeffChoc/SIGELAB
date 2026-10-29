package universidad.sigelab.view;

import universidad.sigelab.controller.ReporteController;
import universidad.sigelab.enums.FiltroPrestamos;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.ResultadoReporte;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

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

    private static final Color AZUL_MARINO = new Color(23, 54, 93);
    private static final Color AZUL_SELECCION = new Color(226, 236, 248);
    private static final Color FONDO = new Color(245, 247, 250);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(35, 42, 52);
    private static final Color BORDE = new Color(218, 224, 232);
    private static final Color AZUL_BOTON = new Color(37, 99, 235);

    private final ReporteController controladorReporte =
            new ReporteController();

    private final JComboBox<String> comboReporte =
            new JComboBox<>(REPORTES);

    private final JComboBox<FiltroPrestamos> comboFiltroPrestamos =
            new JComboBox<>(FiltroPrestamos.values());

    private final JTextField campoCodigoEquipo = new JTextField();

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

    private final JButton botonConsultar =
            new JButton("Consultar");

    public ReporteFrame() {
        super("SIGELAB - Consultas y reportes");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        construirInterfaz();
        actualizarParametros();

        setMinimumSize(new Dimension(1000, 600));
        setSize(1200, 680);
        setLocationRelativeTo(null);

        EstiloSIGELAB.aplicar(this);
    }

    private void construirInterfaz() {
        spinnerDesde.setEditor(
                new JSpinner.DateEditor(spinnerDesde, "dd/MM/yyyy")
        );

        spinnerHasta.setEditor(
                new JSpinner.DateEditor(spinnerHasta, "dd/MM/yyyy")
        );

        configurarComponentes();
        configurarTabla();

        comboReporte.addActionListener(e -> actualizarParametros());

        campoCodigoEquipo.addActionListener(e -> consultar());

        botonConsultar.addActionListener(e -> consultar());

        // Primera fila de filtros.
        JPanel filaUno = new JPanel(new GridBagLayout());
        filaUno.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.weightx = 1;
        filaUno.add(new JLabel("Tipo de reporte:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 2;
        filaUno.add(comboReporte, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        filaUno.add(new JLabel("Estado de préstamo:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        filaUno.add(comboFiltroPrestamos, gbc);

        // Segunda fila de filtros.
        JPanel filaDos = new JPanel(new GridBagLayout());
        filaDos.setOpaque(false);

        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 6, 5, 6);
        gbc2.gridy = 0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.anchor = GridBagConstraints.WEST;

        gbc2.gridx = 0;
        gbc2.weightx = 0;
        filaDos.add(new JLabel("Desde:"), gbc2);

        gbc2.gridx = 1;
        filaDos.add(spinnerDesde, gbc2);

        gbc2.gridx = 2;
        filaDos.add(new JLabel("Hasta:"), gbc2);

        gbc2.gridx = 3;
        filaDos.add(spinnerHasta, gbc2);

        gbc2.gridx = 4;
        gbc2.weightx = 0;
        filaDos.add(new JLabel("Código del equipo:"), gbc2);

        gbc2.gridx = 5;
        gbc2.weightx = 1;
        gbc2.ipadx = 50;
        filaDos.add(campoCodigoEquipo, gbc2);

        // Panel que agrupa las dos filas.
        JPanel panelFiltros = new JPanel(new GridBagLayout());
        panelFiltros.setBackground(BLANCO);
        panelFiltros.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                )
        );

        GridBagConstraints gf = new GridBagConstraints();
        gf.gridx = 0;
        gf.weightx = 1;
        gf.fill = GridBagConstraints.HORIZONTAL;
        gf.anchor = GridBagConstraints.WEST;

        gf.gridy = 0;
        panelFiltros.add(filaUno, gf);

        gf.gridy = 1;
        panelFiltros.add(filaDos, gf);

        JPanel panelAcciones = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );
        panelAcciones.setOpaque(false);
        panelAcciones.add(botonConsultar);

        JPanel panelSuperior = new JPanel(new BorderLayout(0, 8));
        panelSuperior.setBackground(FONDO);
        panelSuperior.add(panelFiltros, BorderLayout.CENTER);
        panelSuperior.add(panelAcciones, BorderLayout.SOUTH);

        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(
                BorderFactory.createLineBorder(BORDE)
        );
        scrollTabla.getViewport().setBackground(BLANCO);

        etiquetaTotal.setForeground(TEXTO);
        etiquetaTotal.setFont(new Font("SansSerif", Font.PLAIN, 12));
        etiquetaTotal.setBorder(
                BorderFactory.createEmptyBorder(8, 4, 2, 4)
        );

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
        comboReporte.setBackground(BLANCO);
        comboReporte.setForeground(TEXTO);
        comboReporte.setFont(new Font("SansSerif", Font.PLAIN, 13));
        comboReporte.setPreferredSize(new Dimension(300, 34));

        comboFiltroPrestamos.setBackground(BLANCO);
        comboFiltroPrestamos.setForeground(TEXTO);
        comboFiltroPrestamos.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );
        comboFiltroPrestamos.setPreferredSize(
                new Dimension(180, 34)
        );

        campoCodigoEquipo.setEditable(true);
        campoCodigoEquipo.setEnabled(true);
        campoCodigoEquipo.setBackground(BLANCO);
        campoCodigoEquipo.setForeground(TEXTO);
        campoCodigoEquipo.setCaretColor(AZUL_MARINO);
        campoCodigoEquipo.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );
        campoCodigoEquipo.setPreferredSize(
                new Dimension(200, 34)
        );
        campoCodigoEquipo.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        BorderFactory.createEmptyBorder(4, 7, 4, 7)
                )
        );
        campoCodigoEquipo.setToolTipText(
                "Escriba el código del equipo para consultar su historial"
        );

        configurarSpinner(spinnerDesde);
        configurarSpinner(spinnerHasta);

        botonConsultar.setFont(
                new Font("SansSerif", Font.BOLD, 13)
        );
        botonConsultar.setForeground(BLANCO);
        botonConsultar.setBackground(AZUL_BOTON);
        botonConsultar.setOpaque(true);
        botonConsultar.setContentAreaFilled(true);
        botonConsultar.setBorderPainted(false);
        botonConsultar.setFocusPainted(false);
        botonConsultar.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        botonConsultar.setPreferredSize(new Dimension(135, 38));
        botonConsultar.setToolTipText(
                "Generar el reporte seleccionado"
        );
    }

    private void configurarSpinner(JSpinner spinner) {
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        spinner.setPreferredSize(new Dimension(125, 34));
        spinner.setBorder(BorderFactory.createLineBorder(BORDE));

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
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 12));
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
        encabezado.setFont(new Font("SansSerif", Font.BOLD, 12));
        encabezado.setPreferredSize(new Dimension(0, 36));
        encabezado.setReorderingAllowed(false);

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

    private void actualizarParametros() {
        int reporte = comboReporte.getSelectedIndex();

        boolean esPrestamoEstado =
                reporte == PRESTAMOS_POR_ESTADO;

        boolean esPrestamoFechas =
                reporte == PRESTAMOS_ENTRE_FECHAS;

        boolean requiereCodigo =
                reporte == HISTORIAL_PRESTAMOS
                        || reporte == HISTORIAL_MANTENIMIENTO;

        comboFiltroPrestamos.setEnabled(esPrestamoEstado);

        spinnerDesde.setEnabled(esPrestamoFechas);
        spinnerHasta.setEnabled(esPrestamoFechas);

        // El campo se habilita solo para los dos historiales por equipo.
        campoCodigoEquipo.setEnabled(requiereCodigo);
        campoCodigoEquipo.setEditable(requiereCodigo);

        comboFiltroPrestamos.setBackground(
                esPrestamoEstado ? BLANCO : new Color(240, 242, 245)
        );

        spinnerDesde.setBackground(
                esPrestamoFechas ? BLANCO : new Color(240, 242, 245)
        );

        spinnerHasta.setBackground(
                esPrestamoFechas ? BLANCO : new Color(240, 242, 245)
        );

        campoCodigoEquipo.setBackground(
                requiereCodigo ? BLANCO : new Color(240, 242, 245)
        );

        if (requiereCodigo) {
            SwingUtilities.invokeLater(
                    () -> campoCodigoEquipo.requestFocusInWindow()
            );
        }
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
                                "La fecha inicial no puede ser posterior "
                                        + "a la fecha final.",
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
                    if (!validarCodigoEquipo()) {
                        return;
                    }

                    resultado =
                            controladorReporte.historialPrestamosDeEquipo(
                                    campoCodigoEquipo.getText().trim()
                            );
                    break;

                case HISTORIAL_MANTENIMIENTO:
                    if (!validarCodigoEquipo()) {
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

    private boolean validarCodigoEquipo() {
        String codigo = campoCodigoEquipo.getText().trim();

        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese el código del equipo.",
                    "Dato requerido",
                    JOptionPane.WARNING_MESSAGE
            );

            campoCodigoEquipo.requestFocusInWindow();
            return false;
        }

        return true;
    }

    private void mostrar(ResultadoReporte resultado) {
        Object[][] filas =
                resultado.getFilas().toArray(new Object[0][]);

        modeloTabla.setDataVector(
                filas,
                resultado.getColumnas().toArray()
        );

        configurarTabla();

        etiquetaTotal.setText(
                filas.length + " registro(s) encontrado(s)."
        );
    }

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
