package universidad.sigelab.view;

import universidad.sigelab.controller.DevolucionController;
import universidad.sigelab.enums.CondicionDevolucion;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.DetallePrestamo;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.PrestamoResumen;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Devoluciones totales o parciales. Solo captura y muestra: las reglas viven en DevolucionService. */
public class DevolucionView extends JFrame {

    // Estos nombres deben coincidir con el "field name" de cada componente en el diseñador (.form)
    private JPanel Panelprincipal;
    private JTextField textBuscarEquipo;
    private JButton btBuscarEquipo;
    private JButton btActualizar;
    private JTable tablaPrestamosActivos;     // dentro de un JScrollPane
    private JLabel labelPrestamo;
    private JTable tablaPendientes;           // dentro de un JScrollPane
    private JComboBox<CondicionDevolucion> comboCondicion;
    private JTextField textObservaciones;
    private JButton btRegistrar;

    private static final String[] COLUMNAS_PRESTAMOS = {
            "N.º", "Solicitante", "Devolución esperada", "Equipos", "Pendientes", "Vencido"};
    private static final String[] COLUMNAS_PENDIENTES = {"Código", "Nombre", "Tipo", "Laboratorio"};
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TEXTO_SIN_PRESTAMO = "Seleccione un préstamo de la lista.";

    private final DevolucionController controladorDevolucion = new DevolucionController();

    private final DefaultTableModel modeloPrestamos = new DefaultTableModel(COLUMNAS_PRESTAMOS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final DefaultTableModel modeloPendientes = new DefaultTableModel(COLUMNAS_PENDIENTES, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private List<PrestamoResumen> prestamosActivos = new ArrayList<>();
    private List<DetallePrestamo> pendientes = new ArrayList<>();

    /** 0 = ningún préstamo seleccionado. */
    private int idPrestamoSeleccionado = 0;

    public DevolucionView() {
        super("SIGELAB - Devoluciones");
        setContentPane(Panelprincipal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarComponentes();
        registrarEventos();
        refrescarPrestamos();

        setSize(1000, 640);
        setLocationRelativeTo(null);
    }

    // ---------- Configuración inicial ----------

    private void configurarComponentes() {
        tablaPrestamosActivos.setModel(modeloPrestamos);
        tablaPrestamosActivos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tablaPendientes.setModel(modeloPendientes);
        tablaPendientes.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        comboCondicion.removeAllItems();
        comboCondicion.addItem(CondicionDevolucion.BUENO);
        comboCondicion.addItem(CondicionDevolucion.DANADO);
        comboCondicion.setSelectedItem(CondicionDevolucion.BUENO);

        labelPrestamo.setText(TEXTO_SIN_PRESTAMO);
    }

    private void registrarEventos() {
        tablaPrestamosActivos.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                alSeleccionarPrestamo();
            }
        });
        btBuscarEquipo.addActionListener(evento -> buscarPorEquipo());
        textBuscarEquipo.addActionListener(evento -> buscarPorEquipo()); // Enter, como lo envía un lector QR
        btActualizar.addActionListener(evento -> refrescarPrestamos());
        btRegistrar.addActionListener(evento -> registrarDevolucion());
    }

    // ---------- Préstamos activos ----------

    private void refrescarPrestamos() {
        try {
            prestamosActivos = new ArrayList<>(controladorDevolucion.listarPrestamosActivos());
            modeloPrestamos.setRowCount(0);
            for (PrestamoResumen resumen : prestamosActivos) {
                modeloPrestamos.addRow(new Object[]{
                        resumen.getIdPrestamo(),
                        resumen.getSolicitanteNombre(),
                        resumen.getFechaEsperadaDevolucion().format(FORMATO_FECHA),
                        resumen.getTotalEquipos(),
                        resumen.getEquiposPendientes(),
                        resumen.estaVencido() ? "Sí" : "No"});
            }
        } catch (Exception e) {
            mostrarError(e);
        }
        limpiarPendientes();
    }

    private void alSeleccionarPrestamo() {
        int fila = tablaPrestamosActivos.getSelectedRow();
        if (fila < 0 || fila >= prestamosActivos.size()) {
            return;
        }
        PrestamoResumen resumen = prestamosActivos.get(tablaPrestamosActivos.convertRowIndexToModel(fila));
        idPrestamoSeleccionado = resumen.getIdPrestamo();
        labelPrestamo.setText("Préstamo N.º " + resumen.getIdPrestamo() + " - " + resumen.getSolicitanteNombre()
                + ": seleccione los equipos que regresan.");
        cargarPendientes();
    }

    /** Selecciona el préstamo en la tabla; devuelve false si no está en la lista de activos. */
    private boolean seleccionarPrestamo(int idPrestamo) {
        for (int i = 0; i < prestamosActivos.size(); i++) {
            if (prestamosActivos.get(i).getIdPrestamo() == idPrestamo) {
                tablaPrestamosActivos.setRowSelectionInterval(i, i);
                return true;
            }
        }
        return false;
    }

    // ---------- Equipos pendientes ----------

    private void cargarPendientes() {
        try {
            pendientes = new ArrayList<>(controladorDevolucion.listarPendientes(idPrestamoSeleccionado));
            modeloPendientes.setRowCount(0);
            for (DetallePrestamo detalle : pendientes) {
                Equipo equipo = detalle.getEquipo();
                modeloPendientes.addRow(new Object[]{
                        equipo.getCodigoInventario(),
                        equipo.getNombre(),
                        equipo.getTipoEquipo().getNombre(),
                        equipo.getLaboratorio().getCodigo()});
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

    // ---------- Acciones ----------

    private void buscarPorEquipo() {
        try {
            int idPrestamo = controladorDevolucion.buscarPrestamoPorCodigoEquipo(textBuscarEquipo.getText());
            refrescarPrestamos();
            if (!seleccionarPrestamo(idPrestamo)) {
                JOptionPane.showMessageDialog(this, "El préstamo N.º " + idPrestamo
                                + " ya no aparece como activo. Actualice la lista.",
                        "Devoluciones", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void registrarDevolucion() {
        if (idPrestamoSeleccionado == 0) {
            JOptionPane.showMessageDialog(this, TEXTO_SIN_PRESTAMO,
                    "Devoluciones", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Integer> idsDetalle = new ArrayList<>();
        for (int fila : tablaPendientes.getSelectedRows()) {
            idsDetalle.add(pendientes.get(tablaPendientes.convertRowIndexToModel(fila)).getIdDetalle());
        }

        try {
            int idPrestamo = idPrestamoSeleccionado;
            int restantes = controladorDevolucion.registrarDevolucion(idPrestamo, idsDetalle,
                    (CondicionDevolucion) comboCondicion.getSelectedItem(), textObservaciones.getText());

            String mensaje = restantes == 0
                    ? "Devolución registrada. El préstamo N.º " + idPrestamo + " quedó FINALIZADO."
                    : "Devolución registrada. Quedan " + restantes + " equipo(s) pendiente(s) en el préstamo N.º "
                    + idPrestamo + ".";
            JOptionPane.showMessageDialog(this, mensaje, "Devoluciones", JOptionPane.INFORMATION_MESSAGE);

            textObservaciones.setText("");
            textBuscarEquipo.setText("");
            comboCondicion.setSelectedItem(CondicionDevolucion.BUENO);
            refrescarPrestamos();
            if (restantes > 0) {
                seleccionarPrestamo(idPrestamo); // se queda en el mismo préstamo para seguir devolviendo
            }
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(this, ManejadorErrores.mensajeParaUsuario(e),
                "No se pudo completar la operación", JOptionPane.WARNING_MESSAGE);
    }
}
