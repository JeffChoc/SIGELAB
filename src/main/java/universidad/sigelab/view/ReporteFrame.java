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
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/** Consultas y reportes. Solo captura parámetros y muestra el resultado: el SQL vive en el Repository. */
public class ReporteFrame extends JFrame {

    private static final String[] REPORTES = {
            "Equipos por estado",
            "Equipos por laboratorio y estado",
            "Préstamos (activos, finalizados, vencidos)",
            "Préstamos entre fechas",
            "Historial de préstamos de un equipo",
            "Historial de mantenimiento de un equipo"};

    private static final int EQUIPOS_POR_ESTADO = 0;
    private static final int EQUIPOS_POR_LABORATORIO = 1;
    private static final int PRESTAMOS_POR_ESTADO = 2;
    private static final int PRESTAMOS_ENTRE_FECHAS = 3;
    private static final int HISTORIAL_PRESTAMOS = 4;
    private static final int HISTORIAL_MANTENIMIENTO = 5;

    private final ReporteController controladorReporte = new ReporteController();

    private final JComboBox<String> comboReporte = new JComboBox<>(REPORTES);
    private final JComboBox<FiltroPrestamos> comboFiltroPrestamos = new JComboBox<>(FiltroPrestamos.values());
    private final JTextField campoCodigoEquipo = new JTextField(12);
    private final JSpinner spinnerDesde = new JSpinner(
            new SpinnerDateModel(fechaHaceDias(30), null, null, Calendar.DAY_OF_MONTH));
    private final JSpinner spinnerHasta = new JSpinner(
            new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));

    private final DefaultTableModel modeloTabla = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);
    private final JLabel etiquetaTotal = new JLabel("Seleccione un reporte y pulse Consultar.");

    public ReporteFrame() {
        super("SIGELAB - Consultas y reportes");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construirInterfaz();
        actualizarParametros();
        setSize(1100, 620);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        spinnerDesde.setEditor(new JSpinner.DateEditor(spinnerDesde, "dd/MM/yyyy"));
        spinnerHasta.setEditor(new JSpinner.DateEditor(spinnerHasta, "dd/MM/yyyy"));
        comboReporte.addActionListener(evento -> actualizarParametros());
        campoCodigoEquipo.addActionListener(evento -> consultar()); // Enter, como lo envía un lector QR

        JButton botonConsultar = new JButton("Consultar");
        botonConsultar.addActionListener(evento -> consultar());

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panelSuperior.setBorder(BorderFactory.createTitledBorder("Reporte"));
        panelSuperior.add(comboReporte);
        panelSuperior.add(new JLabel("Préstamos:"));
        panelSuperior.add(comboFiltroPrestamos);
        panelSuperior.add(new JLabel("Desde:"));
        panelSuperior.add(spinnerDesde);
        panelSuperior.add(new JLabel("Hasta:"));
        panelSuperior.add(spinnerHasta);
        panelSuperior.add(new JLabel("Código del equipo:"));
        panelSuperior.add(campoCodigoEquipo);
        panelSuperior.add(botonConsultar);

        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contenido.add(panelSuperior, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);
        contenido.add(etiquetaTotal, BorderLayout.SOUTH);
        setContentPane(contenido);
    }

    /** Solo se habilitan los parámetros que el reporte elegido necesita. */
    private void actualizarParametros() {
        int reporte = comboReporte.getSelectedIndex();
        comboFiltroPrestamos.setEnabled(reporte == PRESTAMOS_POR_ESTADO);
        spinnerDesde.setEnabled(reporte == PRESTAMOS_ENTRE_FECHAS);
        spinnerHasta.setEnabled(reporte == PRESTAMOS_ENTRE_FECHAS);
        campoCodigoEquipo.setEnabled(reporte == HISTORIAL_PRESTAMOS || reporte == HISTORIAL_MANTENIMIENTO);
    }

    private void consultar() {
        try {
            ResultadoReporte resultado;
            switch (comboReporte.getSelectedIndex()) {
                case EQUIPOS_POR_ESTADO:
                    resultado = controladorReporte.equiposPorEstado();
                    break;
                case EQUIPOS_POR_LABORATORIO:
                    resultado = controladorReporte.equiposPorLaboratorioYEstado();
                    break;
                case PRESTAMOS_POR_ESTADO:
                    resultado = controladorReporte.prestamos((FiltroPrestamos) comboFiltroPrestamos.getSelectedItem());
                    break;
                case PRESTAMOS_ENTRE_FECHAS:
                    resultado = controladorReporte.prestamosEntreFechas(
                            aFecha(spinnerDesde), aFecha(spinnerHasta));
                    break;
                case HISTORIAL_PRESTAMOS:
                    resultado = controladorReporte.historialPrestamosDeEquipo(campoCodigoEquipo.getText());
                    break;
                case HISTORIAL_MANTENIMIENTO:
                    resultado = controladorReporte.historialMantenimientoDeEquipo(campoCodigoEquipo.getText());
                    break;
                default:
                    return;
            }
            mostrar(resultado);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, ManejadorErrores.mensajeParaUsuario(e),
                    "No se pudo consultar el reporte", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void mostrar(ResultadoReporte resultado) {
        Object[][] filas = resultado.getFilas().toArray(new Object[0][]);
        modeloTabla.setDataVector(filas, resultado.getColumnas().toArray());
        etiquetaTotal.setText(filas.length + " registro(s) encontrado(s).");
    }

    // ---------- Utilidades ----------

    private static Date fechaHaceDias(int dias) {
        return Date.from(LocalDate.now().minusDays(dias).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate aFecha(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
