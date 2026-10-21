package universidad.sigelab.view;

import universidad.sigelab.controller.TipoEquipoController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/** Pantalla del catálogo de tipos de equipo. Las reglas viven en TipoEquipoService. */
public class TipoEquipoView extends JFrame {

    // Estos nombres deben coincidir con el "field name" de cada componente en el diseñador (.form)
    private JPanel Panelprincipal;
    private JTable tablaTipos;                 // dentro de un JScrollPane
    private JTextField textNombre;
    private JTextField textDescripcion;
    private JButton btNuevo;
    private JButton btGuardar;

    private static final String[] COLUMNAS = {"Nombre", "Descripción"};

    private final TipoEquipoController controladorTipoEquipo = new TipoEquipoController();

    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private List<TipoEquipo> tiposMostrados = new ArrayList<>();

    /** 0 = se está registrando uno nuevo; distinto de 0 = se está editando ese tipo. */
    private int idTipoSeleccionado = 0;

    public TipoEquipoView() {
        super("SIGELAB - Tipos de equipo");
        setContentPane(Panelprincipal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarTabla();
        registrarEventos();
        refrescarTabla();

        setSize(750, 520);
        setLocationRelativeTo(null);
    }

    // ---------- Configuración inicial ----------

    private void configurarTabla() {
        tablaTipos.setModel(modeloTabla);
        tablaTipos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaTipos.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                alSeleccionarFila();
            }
        });
    }

    private void registrarEventos() {
        btNuevo.addActionListener(evento -> limpiarFormulario());
        btGuardar.addActionListener(evento -> guardar());
    }

    // ---------- Tabla ----------

    private void refrescarTabla() {
        try {
            tiposMostrados = new ArrayList<>(controladorTipoEquipo.listar());
            modeloTabla.setRowCount(0);
            for (TipoEquipo tipo : tiposMostrados) {
                modeloTabla.addRow(new Object[]{tipo.getNombre(), tipo.getDescripcion()});
            }
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void alSeleccionarFila() {
        int fila = tablaTipos.getSelectedRow();
        if (fila < 0 || fila >= tiposMostrados.size()) {
            return;
        }
        TipoEquipo tipo = tiposMostrados.get(tablaTipos.convertRowIndexToModel(fila));
        idTipoSeleccionado = tipo.getIdTipoEquipo();
        textNombre.setText(tipo.getNombre());
        textDescripcion.setText(tipo.getDescripcion());
    }

    // ---------- Acciones ----------

    private void limpiarFormulario() {
        idTipoSeleccionado = 0;
        tablaTipos.clearSelection();
        textNombre.setText("");
        textDescripcion.setText("");
        textNombre.requestFocusInWindow();
    }

    private void guardar() {
        try {
            TipoEquipo guardado;
            if (idTipoSeleccionado == 0) {
                guardado = controladorTipoEquipo.registrar(textNombre.getText(), textDescripcion.getText());
            } else {
                guardado = controladorTipoEquipo.modificar(idTipoSeleccionado,
                        textNombre.getText(), textDescripcion.getText());
            }
            JOptionPane.showMessageDialog(this,
                    "El tipo de equipo " + guardado.getNombre() + " se guardó correctamente.",
                    "Tipos de equipo", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            refrescarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(this, ManejadorErrores.mensajeParaUsuario(e),
                "No se pudo completar la operación", JOptionPane.WARNING_MESSAGE);
    }
}