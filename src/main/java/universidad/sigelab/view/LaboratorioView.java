package universidad.sigelab.view;

import universidad.sigelab.controller.LaboratorioController;
import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Laboratorio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/** Pantalla de laboratorios. Solo captura y muestra: las reglas viven en LaboratorioService. */
public class LaboratorioView extends JFrame {

    // Estos nombres deben coincidir con el "field name" de cada componente en el diseñador (.form)
    private JPanel Panelprincipal;
    private JTable tablaLaboratorios;          // dentro de un JScrollPane
    private JTextField textCodigo;
    private JTextField textNombre;
    private JTextField textUbicacion;
    private JTextField textCapacidad;
    private JLabel labelEstado;
    private JButton btNuevo;
    private JButton btGuardar;
    private JButton btCambiarEstado;

    private static final String[] COLUMNAS = {"Código", "Nombre", "Ubicación", "Capacidad", "Estado"};

    private final LaboratorioController controladorLaboratorio = new LaboratorioController();

    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private List<Laboratorio> laboratoriosMostrados = new ArrayList<>();

    /** 0 = se está registrando uno nuevo; distinto de 0 = se está editando ese laboratorio. */
    private int idLaboratorioSeleccionado = 0;

    public LaboratorioView() {
        super("SIGELAB - Laboratorios");
        setContentPane(Panelprincipal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarTabla();
        registrarEventos();
        refrescarTabla();

        setSize(900, 560);
        setLocationRelativeTo(null);
        EstiloSIGELAB.aplicar(this);
    }

    // ---------- Configuración inicial ----------

    private void configurarTabla() {
        tablaLaboratorios.setModel(modeloTabla);
        tablaLaboratorios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaLaboratorios.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                alSeleccionarFila();
            }
        });
    }

    private void registrarEventos() {
        btNuevo.addActionListener(evento -> limpiarFormulario());
        btGuardar.addActionListener(evento -> guardar());
        btCambiarEstado.addActionListener(evento -> cambiarEstado());
        labelEstado.setText("-");
        actualizarBotonEstado(null);
    }

    // ---------- Tabla ----------

    private void refrescarTabla() {
        try {
            laboratoriosMostrados = new ArrayList<>(controladorLaboratorio.listar());
            modeloTabla.setRowCount(0);
            for (Laboratorio laboratorio : laboratoriosMostrados) {
                modeloTabla.addRow(new Object[]{
                        laboratorio.getCodigo(),
                        laboratorio.getNombre(),
                        laboratorio.getUbicacion(),
                        laboratorio.getCapacidad(),
                        laboratorio.getEstado().name()});
            }
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void alSeleccionarFila() {
        int fila = tablaLaboratorios.getSelectedRow();
        if (fila < 0 || fila >= laboratoriosMostrados.size()) {
            return;
        }
        Laboratorio laboratorio = laboratoriosMostrados.get(tablaLaboratorios.convertRowIndexToModel(fila));
        idLaboratorioSeleccionado = laboratorio.getIdLaboratorio();
        textCodigo.setText(laboratorio.getCodigo());
        textNombre.setText(laboratorio.getNombre());
        textUbicacion.setText(laboratorio.getUbicacion());
        textCapacidad.setText(String.valueOf(laboratorio.getCapacidad()));
        labelEstado.setText(laboratorio.getEstado().name());
        actualizarBotonEstado(laboratorio.getEstado());
    }

    // ---------- Acciones ----------

    private void limpiarFormulario() {
        idLaboratorioSeleccionado = 0;
        tablaLaboratorios.clearSelection();
        textCodigo.setText("");
        textNombre.setText("");
        textUbicacion.setText("");
        textCapacidad.setText("");
        labelEstado.setText("-");
        actualizarBotonEstado(null);
        textCodigo.requestFocusInWindow();
    }

    private void guardar() {
        try {
            Laboratorio guardado;
            if (idLaboratorioSeleccionado == 0) {
                guardado = controladorLaboratorio.registrar(textCodigo.getText(), textNombre.getText(),
                        textUbicacion.getText(), textCapacidad.getText());
            } else {
                guardado = controladorLaboratorio.modificar(idLaboratorioSeleccionado, textCodigo.getText(),
                        textNombre.getText(), textUbicacion.getText(), textCapacidad.getText());
            }
            JOptionPane.showMessageDialog(this,
                    "El laboratorio " + guardado.getCodigo() + " se guardó correctamente.",
                    "Laboratorios", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            refrescarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    /** Desactivación lógica: el laboratorio no se borra para conservar el historial de sus equipos. */
    private void cambiarEstado() {
        if (idLaboratorioSeleccionado == 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un laboratorio de la tabla.",
                    "Laboratorios", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EstadoRegistro estadoActual = EstadoRegistro.valueOf(labelEstado.getText());
        EstadoRegistro nuevoEstado = estadoActual == EstadoRegistro.ACTIVO
                ? EstadoRegistro.INACTIVO : EstadoRegistro.ACTIVO;
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Desea dejar el laboratorio " + textCodigo.getText() + " como " + nuevoEstado + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controladorLaboratorio.cambiarEstado(idLaboratorioSeleccionado, nuevoEstado);
            limpiarFormulario();
            refrescarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    /**
     * El botón muestra solo la acción que corresponde: "Desactivar" si el laboratorio está ACTIVO,
     * "Activar" si está INACTIVO. Sin selección queda deshabilitado.
     */
    private void actualizarBotonEstado(EstadoRegistro estadoSeleccionado) {
        if (estadoSeleccionado == EstadoRegistro.ACTIVO) {
            btCambiarEstado.setText("Desactivar");
            btCambiarEstado.setEnabled(true);
        } else if (estadoSeleccionado == EstadoRegistro.INACTIVO) {
            btCambiarEstado.setText("Activar");
            btCambiarEstado.setEnabled(true);
        } else {
            btCambiarEstado.setText("Cambiar estado");
            btCambiarEstado.setEnabled(false);
        }
    }

    private void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(this, ManejadorErrores.mensajeParaUsuario(e),
                "No se pudo completar la operación", JOptionPane.WARNING_MESSAGE);
    }
}