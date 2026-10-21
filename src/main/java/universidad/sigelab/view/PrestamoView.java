package universidad.sigelab.view;

import universidad.sigelab.controller.PrestamoController;
import universidad.sigelab.controller.QrController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.PrestamoResumen;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Préstamos con varios equipos. Solo captura y muestra: las reglas viven en PrestamoService. */
public class PrestamoView extends JFrame {

    // Estos nombres deben coincidir con el "field name" de cada componente en el diseñador (.form)
    private JPanel Panelprincipal;
    private JTabbedPane tabPrincipal;
    private JTextField textSolicitante;
    private JTextField textIdentificacion;
    private JSpinner spinnerFechaEsperada;
    private JTextField textObservaciones;
    private JTextField textCodigoEquipo;
    private JButton btAgregarEquipo;
    private JButton btQuitarEquipo;
    private JButton btLeerQr;                // NUEVO: lee el QR del equipo desde una imagen
    private JTable tablaEquiposPrestamo;     // dentro de un JScrollPane
    private JButton btLimpiar;
    private JButton btConfirmar;
    private JTable tablaPrestamos;           // dentro de un JScrollPane, en la segunda pestaña
    private JButton btActualizar;

    private static final String[] COLUMNAS_EQUIPOS = {"Código", "Nombre", "Tipo", "Laboratorio", "Estado"};
    private static final String[] COLUMNAS_PRESTAMOS = {
            "N.º", "Solicitante", "Fecha del préstamo", "Devolución esperada",
            "Estado", "Equipos", "Pendientes", "Vencido"};
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PrestamoController controladorPrestamo = new PrestamoController();
    private final QrController controladorQr = new QrController();
    private final List<Equipo> equiposDelPrestamo = new ArrayList<>();

    private final DefaultTableModel modeloEquipos = new DefaultTableModel(COLUMNAS_EQUIPOS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final DefaultTableModel modeloPrestamos = new DefaultTableModel(COLUMNAS_PRESTAMOS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public PrestamoView() {
        super("SIGELAB - Préstamos");
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
        spinnerFechaEsperada.setModel(new SpinnerDateModel(fechaPorDefecto(), null, null, Calendar.DAY_OF_MONTH));
        spinnerFechaEsperada.setEditor(new JSpinner.DateEditor(spinnerFechaEsperada, "dd/MM/yyyy HH:mm"));

        tablaEquiposPrestamo.setModel(modeloEquipos);
        tablaEquiposPrestamo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPrestamos.setModel(modeloPrestamos);
    }

    private void registrarEventos() {
        btAgregarEquipo.addActionListener(evento -> agregarEquipo());
        textCodigoEquipo.addActionListener(evento -> agregarEquipo()); // Enter, como lo envía un lector QR
        btQuitarEquipo.addActionListener(evento -> quitarEquipo());
        btLeerQr.addActionListener(evento -> leerQr());
        btLimpiar.addActionListener(evento -> limpiarFormulario());
        btConfirmar.addActionListener(evento -> confirmarPrestamo());
        btActualizar.addActionListener(evento -> refrescarPrestamos());
        tabPrincipal.addChangeListener(evento -> {
            if (tabPrincipal.getSelectedIndex() == 1) {
                refrescarPrestamos();
            }
        });
    }

    // ---------- Acciones ----------

    private void agregarEquipo() {
        try {
            Equipo equipo = controladorPrestamo.identificarEquipo(textCodigoEquipo.getText(), equiposDelPrestamo);
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

    /**
     * Lee el QR de una imagen y usa su valor para consultar el equipo real en la base de datos.
     * RN-19: el QR solo identifica; el estado no cambia hasta confirmar el préstamo.
     */
    private void leerQr() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Seleccione la imagen del código QR");
        selector.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (png, jpg, jpeg, gif, bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            String codigo = controladorQr.leerCodigo(selector.getSelectedFile());
            textCodigoEquipo.setText(codigo);
            agregarEquipo(); // busca el equipo, valida que esté DISPONIBLE y que no esté repetido
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void quitarEquipo() {
        int fila = tablaEquiposPrestamo.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un equipo de la lista.",
                    "Préstamos", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        equiposDelPrestamo.remove(tablaEquiposPrestamo.convertRowIndexToModel(fila));
        llenarTablaEquipos();
    }

    private void confirmarPrestamo() {
        try {
            int cantidadEquipos = equiposDelPrestamo.size();
            int idPrestamo = controladorPrestamo.confirmarPrestamo(
                    textSolicitante.getText(), textIdentificacion.getText(), fechaEsperada(),
                    textObservaciones.getText(), equiposDelPrestamo);
            JOptionPane.showMessageDialog(this,
                    "Préstamo N.º " + idPrestamo + " registrado con " + cantidadEquipos + " equipo(s).",
                    "Préstamos", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            refrescarPrestamos();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void limpiarFormulario() {
        textSolicitante.setText("");
        textIdentificacion.setText("");
        spinnerFechaEsperada.setValue(fechaPorDefecto());
        textObservaciones.setText("");
        textCodigoEquipo.setText("");
        equiposDelPrestamo.clear();
        llenarTablaEquipos();
        textSolicitante.requestFocusInWindow();
    }

    // ---------- Tablas ----------

    private void llenarTablaEquipos() {
        modeloEquipos.setRowCount(0);
        for (Equipo equipo : equiposDelPrestamo) {
            modeloEquipos.addRow(new Object[]{
                    equipo.getCodigoInventario(),
                    equipo.getNombre(),
                    equipo.getTipoEquipo().getNombre(),
                    equipo.getLaboratorio().getCodigo(),
                    equipo.getEstado().name()});
        }
    }

    private void refrescarPrestamos() {
        try {
            modeloPrestamos.setRowCount(0);
            for (PrestamoResumen resumen : controladorPrestamo.listarResumen()) {
                modeloPrestamos.addRow(new Object[]{
                        resumen.getIdPrestamo(),
                        resumen.getSolicitanteNombre(),
                        resumen.getFechaPrestamo().format(FORMATO_FECHA),
                        resumen.getFechaEsperadaDevolucion().format(FORMATO_FECHA),
                        resumen.getEstado().name(),
                        resumen.getTotalEquipos(),
                        resumen.getEquiposPendientes(),
                        resumen.estaVencido() ? "Sí" : "No"});
            }
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // ---------- Utilidades ----------

    private static Date fechaPorDefecto() {
        return Date.from(LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime fechaEsperada() {
        Date fecha = (Date) spinnerFechaEsperada.getValue();
        return LocalDateTime.ofInstant(fecha.toInstant(), ZoneId.systemDefault());
    }

    private void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(this, ManejadorErrores.mensajeParaUsuario(e),
                "No se pudo completar la operación", JOptionPane.WARNING_MESSAGE);
    }
}
