package universidad.sigelab.view;

import universidad.sigelab.controller.AuditoriaController;
import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.ResultadoReporte;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Consulta de auditoría (RN-18).
 * Compatible con el formulario visual AuditoriaView.form.
 */
public class AuditoriaView extends JFrame {

    // Estos nombres deben coincidir con los del archivo .form.
    private JPanel Panelprincipal;
    private JComboBox<String> comboAccion;
    private JTextField textUsuario;
    private JCheckBox checkFechas;
    private JSpinner spinnerDesde;
    private JSpinner spinnerHasta;
    private JButton btConsultar;
    private JTable tablaAuditoria;
    private JLabel labelTotal;

    private static final String OPCION_TODAS = "TODAS";

    private final AuditoriaController controladorAuditoria =
            new AuditoriaController();

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    public AuditoriaView() {
        super("SIGELAB - Auditoría");

        // IntelliJ inicializa los campos vinculados al formulario.
        setContentPane(Panelprincipal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarComponentes();
        registrarEventos();

        setSize(1150, 620);
        setLocationRelativeTo(null);
        EstiloSIGELAB.aplicar(this);

        // Carga los registros recientes al abrir.
        consultar();
    }

    /**
     * Configuración inicial de los componentes del formulario.
     */
    private void configurarComponentes() {

        comboAccion.removeAllItems();
        comboAccion.addItem(OPCION_TODAS);

        for (AccionAuditoria accion : AccionAuditoria.values()) {
            comboAccion.addItem(accion.name());
        }

        spinnerDesde.setModel(
                new SpinnerDateModel(
                        fechaHaceDias(7),
                        null,
                        null,
                        Calendar.DAY_OF_MONTH
                )
        );

        spinnerHasta.setModel(
                new SpinnerDateModel(
                        new Date(),
                        null,
                        null,
                        Calendar.DAY_OF_MONTH
                )
        );

        spinnerDesde.setEditor(
                new JSpinner.DateEditor(spinnerDesde, "dd/MM/yyyy")
        );

        spinnerHasta.setEditor(
                new JSpinner.DateEditor(spinnerHasta, "dd/MM/yyyy")
        );

        checkFechas.setSelected(false);
        spinnerDesde.setEnabled(false);
        spinnerHasta.setEnabled(false);

        tablaAuditoria.setModel(modeloTabla);
        tablaAuditoria.setRowHeight(24);
        tablaAuditoria.setAutoCreateRowSorter(true);

        labelTotal.setText("Listo para consultar.");
    }

    /**
     * Eventos de los filtros y del botón de consulta.
     */
    private void registrarEventos() {

        checkFechas.addActionListener(evento -> {
            boolean habilitado = checkFechas.isSelected();

            spinnerDesde.setEnabled(habilitado);
            spinnerHasta.setEnabled(habilitado);
        });

        btConsultar.addActionListener(evento -> consultar());

        textUsuario.addActionListener(evento -> consultar());
    }

    /**
     * Consulta los registros de auditoría.
     * La consulta corre en segundo plano para que la ventana no se congele
     * mientras SQL Server responde.
     */
    private void consultar() {

        if (!btConsultar.isEnabled()) {
            return; // ya hay una consulta en curso
        }

        final AccionAuditoria accion;
        final LocalDate desde;
        final LocalDate hasta;
        final String usuario;

        try {
            String seleccion = (String) comboAccion.getSelectedItem();

            accion = (seleccion == null || OPCION_TODAS.equals(seleccion))
                    ? null
                    : AccionAuditoria.valueOf(seleccion);

            desde = checkFechas.isSelected() ? aFecha(spinnerDesde) : null;
            hasta = checkFechas.isSelected() ? aFecha(spinnerHasta) : null;
            usuario = textUsuario.getText().trim();

        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(
                    this,
                    ManejadorErrores.mensajeParaUsuario(e),
                    "Filtros inválidos",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            JOptionPane.showMessageDialog(
                    this,
                    "La fecha inicial no puede ser posterior a la fecha final.",
                    "Fechas inválidas",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        btConsultar.setEnabled(false);
        labelTotal.setText("Consultando...");

        new SwingWorker<ResultadoReporte, Void>() {

            @Override
            protected ResultadoReporte doInBackground() {
                return controladorAuditoria.consultar(
                        desde,
                        hasta,
                        accion,
                        usuario
                );
            }

            @Override
            protected void done() {
                try {
                    ResultadoReporte resultado = get();

                    Object[][] filas = resultado.getFilas()
                            .toArray(new Object[0][]);

                    modeloTabla.setDataVector(
                            filas,
                            resultado.getColumnas().toArray()
                    );

                    labelTotal.setText(
                            filas.length + " registro(s) encontrados."
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    JOptionPane.showMessageDialog(
                            AuditoriaView.this,
                            "La consulta fue interrumpida.",
                            "Auditoría",
                            JOptionPane.WARNING_MESSAGE
                    );

                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;

                    JOptionPane.showMessageDialog(
                            AuditoriaView.this,
                            ManejadorErrores.mensajeParaUsuario(causa),
                            "No se pudo consultar la auditoría",
                            JOptionPane.WARNING_MESSAGE
                    );

                } finally {
                    btConsultar.setEnabled(true);
                }
            }
        }.execute();
    }

    /**
     * Convierte la fecha del selector a LocalDate.
     */
    private LocalDate aFecha(JSpinner spinner) {

        Date fecha = (Date) spinner.getValue();

        return fecha.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * Devuelve la fecha de hace cierta cantidad de días.
     */
    private static Date fechaHaceDias(int dias) {

        return Date.from(
                LocalDate.now()
                        .minusDays(dias)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );
    }
}
