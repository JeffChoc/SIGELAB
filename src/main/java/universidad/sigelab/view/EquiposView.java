package universidad.sigelab.view;

import universidad.sigelab.controller.EquipoController;
import universidad.sigelab.controller.EstadoEquipoController;
import universidad.sigelab.controller.QrController;
import universidad.sigelab.enums.EstadoEquipo;
import universidad.sigelab.enums.ResultadoMantenimiento;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.enums.TipoMantenimiento;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.Laboratorio;
import universidad.sigelab.model.TipoEquipo;
import universidad.sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de equipos. Solo captura y muestra: toda regla vive en EquipoService.
 * Los botones deshabilitados son una ayuda visual; el permiso real lo valida el Service (RN-20).
 */
public class EquiposView extends JFrame {

    // Estos nombres deben coincidir con el "field name" de cada componente en el diseñador (.form)
    private JPanel Panelprincipal;
    private JComboBox<String> comboEstado;
    private JTextField textBuscarCodigo;
    private JButton btBuscar;
    private JButton btVertodo;
    private JTable tablaEquipos;            // en el diseñador: JTable dentro de un JScrollPane
    private JLabel labelEstado;
    private JComboBox<EstadoEquipo> comboEstadoRegistro;   // NUEVO: estado inicial al registrar
    private JTextField textInventario;
    private JComboBox<TipoEquipo> comboTipo;
    private JTextField textModelo;
    private JComboBox<Laboratorio> comboLaboratorio;
    private JTextField textNombre;
    private JTextField textMarca;
    private JTextField textSerie;
    private JTextField textObservaciones;
    private JButton btGuardar;
    private JButton btNuevo;
    private JButton btGenerarQr;               // NUEVO: genera la imagen QR del equipo seleccionado
    private JButton btCambiarEstado;          // NUEVO: cambia el estado de un equipo ya registrado

    private static final String[] COLUMNAS = {
            "Código", "Nombre", "Tipo", "Marca", "Modelo", "N.º de serie", "Laboratorio", "Estado"};
    private static final String OPCION_TODOS = "TODOS";

    private final EquipoController controladorEquipo = new EquipoController();
    private final EstadoEquipoController controladorEstado = new EstadoEquipoController();
    private final QrController controladorQr = new QrController();
    private final boolean puedeEditar;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private List<Equipo> equiposMostrados = new ArrayList<>();

    /** 0 = se está registrando uno nuevo; distinto de 0 = se está editando ese equipo. */
    private int idEquipoSeleccionado = 0;

    public EquiposView(Usuario usuario) {
        super("SIGELAB - Equipos");
        RolUsuario rol = usuario.getRol().getNombre();
        this.puedeEditar = rol == RolUsuario.ADMIN || rol == RolUsuario.ENCARGADO;

        setContentPane(Panelprincipal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarTabla();
        configurarFiltroEstado();
        configurarEstadoRegistro();
        cargarCombosFormulario();
        registrarEventos();
        refrescarTabla();

        setSize(1050, 640);
        setLocationRelativeTo(null);
    }

    // ---------- Configuración inicial ----------

    private void configurarTabla() {
        tablaEquipos.setModel(modeloTabla);
        tablaEquipos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaEquipos.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                alSeleccionarFila();
            }
        });
    }

    private void configurarFiltroEstado() {
        comboEstado.removeAllItems();
        comboEstado.addItem(OPCION_TODOS);
        for (EstadoEquipo estado : EstadoEquipo.values()) {
            comboEstado.addItem(estado.name());
        }
    }

    /** Al registrar se puede elegir DISPONIBLE o DANADO; los demás estados salen de préstamos y mantenimiento. */
    private void configurarEstadoRegistro() {
        comboEstadoRegistro.removeAllItems();
        comboEstadoRegistro.addItem(EstadoEquipo.DISPONIBLE);
        comboEstadoRegistro.addItem(EstadoEquipo.DANADO);
        comboEstadoRegistro.setSelectedItem(EstadoEquipo.DISPONIBLE);
        comboEstadoRegistro.setEnabled(puedeEditar);
    }

    private void cargarCombosFormulario() {
        try {
            comboTipo.removeAllItems();
            for (TipoEquipo tipo : controladorEquipo.tiposParaCombo()) {
                comboTipo.addItem(tipo);
            }
            comboTipo.setSelectedItem(null);

            comboLaboratorio.removeAllItems();
            for (Laboratorio laboratorio : controladorEquipo.laboratoriosParaCombo()) {
                comboLaboratorio.addItem(laboratorio);
            }
            comboLaboratorio.setSelectedItem(null);
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    /** Los eventos se registran al final para que no se disparen mientras se cargan los combos. */
    private void registrarEventos() {
        comboEstado.addActionListener(evento -> refrescarTabla());
        btBuscar.addActionListener(evento -> buscarPorCodigo());
        btVertodo.addActionListener(evento -> verTodos());
        btNuevo.addActionListener(evento -> limpiarFormulario());
        btGuardar.addActionListener(evento -> guardar());

        btCambiarEstado.addActionListener(evento -> cambiarEstado());
        btGenerarQr.addActionListener(evento -> generarQr());
        btGenerarQr.setEnabled(puedeEditar);
        btNuevo.setEnabled(puedeEditar);
        btGuardar.setEnabled(puedeEditar);
        btCambiarEstado.setEnabled(puedeEditar);
        labelEstado.setText("-");
    }

    // ---------- Tabla ----------

    private void refrescarTabla() {
        try {
            String filtro = (String) comboEstado.getSelectedItem();
            List<Equipo> equipos = (filtro == null || OPCION_TODOS.equals(filtro))
                    ? controladorEquipo.listar()
                    : controladorEquipo.listarPorEstado(EstadoEquipo.valueOf(filtro));
            llenarTabla(equipos);
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void llenarTabla(List<Equipo> equipos) {
        equiposMostrados = new ArrayList<>(equipos);
        modeloTabla.setRowCount(0);
        for (Equipo equipo : equiposMostrados) {
            modeloTabla.addRow(new Object[]{
                    equipo.getCodigoInventario(),
                    equipo.getNombre(),
                    equipo.getTipoEquipo().getNombre(),
                    equipo.getMarca(),
                    equipo.getModelo(),
                    equipo.getNumeroSerie(),
                    equipo.getLaboratorio().getCodigo(),
                    equipo.getEstado().name()});
        }
    }

    private void alSeleccionarFila() {
        int fila = tablaEquipos.getSelectedRow();
        if (fila < 0 || fila >= equiposMostrados.size()) {
            return;
        }
        cargarFormulario(equiposMostrados.get(tablaEquipos.convertRowIndexToModel(fila)));
    }

    // ---------- Búsqueda ----------

    private void buscarPorCodigo() {
        try {
            Equipo equipo = controladorEquipo.buscarPorCodigo(textBuscarCodigo.getText());
            llenarTabla(List.of(equipo));
            tablaEquipos.setRowSelectionInterval(0, 0);
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void verTodos() {
        textBuscarCodigo.setText("");
        comboEstado.setSelectedItem(OPCION_TODOS);
        refrescarTabla();
    }

    // ---------- Formulario ----------

    private void cargarFormulario(Equipo equipo) {
        idEquipoSeleccionado = equipo.getIdEquipo();
        textInventario.setText(equipo.getCodigoInventario());
        textNombre.setText(equipo.getNombre());
        seleccionarTipo(equipo.getTipoEquipo().getIdTipoEquipo());
        textMarca.setText(equipo.getMarca());
        textModelo.setText(equipo.getModelo());
        textSerie.setText(equipo.getNumeroSerie());
        seleccionarLaboratorio(equipo.getLaboratorio().getIdLaboratorio());
        textObservaciones.setText(equipo.getObservaciones());
        labelEstado.setText(equipo.getEstado().name());

        // Al editar, el estado no se cambia desde aquí: solo se muestra.
        boolean estadoEnLista = equipo.getEstado() == EstadoEquipo.DISPONIBLE
                || equipo.getEstado() == EstadoEquipo.DANADO;
        comboEstadoRegistro.setSelectedItem(estadoEnLista ? equipo.getEstado() : null);
        comboEstadoRegistro.setEnabled(false);
    }

    private void seleccionarTipo(int idTipoEquipo) {
        comboTipo.setSelectedItem(null);
        for (int i = 0; i < comboTipo.getItemCount(); i++) {
            if (comboTipo.getItemAt(i).getIdTipoEquipo() == idTipoEquipo) {
                comboTipo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarLaboratorio(int idLaboratorio) {
        comboLaboratorio.setSelectedItem(null);
        for (int i = 0; i < comboLaboratorio.getItemCount(); i++) {
            if (comboLaboratorio.getItemAt(i).getIdLaboratorio() == idLaboratorio) {
                comboLaboratorio.setSelectedIndex(i);
                return;
            }
        }
    }

    private void limpiarFormulario() {
        idEquipoSeleccionado = 0;
        tablaEquipos.clearSelection();
        textInventario.setText("");
        textNombre.setText("");
        comboTipo.setSelectedItem(null);
        textMarca.setText("");
        textModelo.setText("");
        textSerie.setText("");
        comboLaboratorio.setSelectedItem(null);
        textObservaciones.setText("");
        labelEstado.setText("-");
        comboEstadoRegistro.setSelectedItem(EstadoEquipo.DISPONIBLE);
        comboEstadoRegistro.setEnabled(puedeEditar);
        textInventario.requestFocusInWindow();
    }

    private void guardar() {
        try {
            TipoEquipo tipo = (TipoEquipo) comboTipo.getSelectedItem();
            Laboratorio laboratorio = (Laboratorio) comboLaboratorio.getSelectedItem();

            Equipo guardado;
            if (idEquipoSeleccionado == 0) {
                guardado = controladorEquipo.registrar(textInventario.getText(), textNombre.getText(), tipo,
                        textMarca.getText(), textModelo.getText(), textSerie.getText(),
                        laboratorio, textObservaciones.getText(),
                        (EstadoEquipo) comboEstadoRegistro.getSelectedItem());
            } else {
                guardado = controladorEquipo.modificar(idEquipoSeleccionado, textInventario.getText(),
                        textNombre.getText(), tipo, textMarca.getText(), textModelo.getText(),
                        textSerie.getText(), laboratorio, textObservaciones.getText());
            }
            JOptionPane.showMessageDialog(this,
                    "El equipo " + guardado.getCodigoInventario() + " se guardó correctamente.",
                    "Equipos", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            refrescarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // ---------- Cambio de estado de un equipo ya registrado ----------

    /** Ofrece solo las acciones válidas para el estado actual. Las reglas las valida EstadoEquipoService. */
    private void cambiarEstado() {
        if (idEquipoSeleccionado == 0) {
            informar("Seleccione un equipo de la tabla.");
            return;
        }
        EstadoEquipo estadoActual = EstadoEquipo.valueOf(labelEstado.getText());
        if (estadoActual == EstadoEquipo.DISPONIBLE) {
            elegirAccionParaDisponible();
        } else if (estadoActual == EstadoEquipo.DANADO) {
            pedirDatosMantenimiento(TipoMantenimiento.CORRECTIVO);
        } else if (estadoActual == EstadoEquipo.MANTENIMIENTO) {
            finalizarMantenimiento();
        } else if (estadoActual == EstadoEquipo.PRESTADO) {
            informar("Un equipo PRESTADO cambia de estado cuando se devuelve (módulo de devoluciones).");
        } else {
            informar("Un equipo dado de baja ya no puede cambiar de estado.");
        }
    }

    private void elegirAccionParaDisponible() {
        String[] opciones = {"Reportar daño", "Enviar a mantenimiento preventivo", "Cancelar"};
        int eleccion = JOptionPane.showOptionDialog(this,
                "¿Qué desea hacer con el equipo " + textInventario.getText() + "?",
                "Cambiar estado", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opciones, opciones[2]);
        if (eleccion == 0) {
            reportarDanio();
        } else if (eleccion == 1) {
            pedirDatosMantenimiento(TipoMantenimiento.PREVENTIVO);
        }
    }

    private void reportarDanio() {
        String descripcion = JOptionPane.showInputDialog(this,
                "Describa el daño del equipo " + textInventario.getText() + ":",
                "Reportar daño", JOptionPane.QUESTION_MESSAGE);
        if (descripcion == null) {
            return; // canceló
        }
        ejecutarCambio(() -> controladorEstado.reportarDanio(idEquipoSeleccionado, descripcion),
                "El equipo quedó en estado DANADO.");
    }

    private void pedirDatosMantenimiento(TipoMantenimiento tipo) {
        JTextField campoDescripcion = new JTextField(30);
        JTextField campoResponsable = new JTextField(30);
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Tipo de mantenimiento: " + tipo));
        panel.add(new JLabel("Descripción del trabajo *"));
        panel.add(campoDescripcion);
        panel.add(new JLabel("Responsable *"));
        panel.add(campoResponsable);

        int respuesta = JOptionPane.showConfirmDialog(this, panel,
                "Enviar " + textInventario.getText() + " a mantenimiento",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }
        ejecutarCambio(() -> controladorEstado.enviarAMantenimiento(idEquipoSeleccionado, tipo,
                        campoDescripcion.getText(), campoResponsable.getText()),
                "El equipo quedó en estado MANTENIMIENTO.");
    }

    private void finalizarMantenimiento() {
        String[] opciones = {"Reparado (queda DISPONIBLE)", "No reparable (queda BAJA)", "Cancelar"};
        int eleccion = JOptionPane.showOptionDialog(this,
                "Resultado del mantenimiento de " + textInventario.getText() + ":",
                "Finalizar mantenimiento", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opciones, opciones[2]);
        if (eleccion != 0 && eleccion != 1) {
            return;
        }
        ResultadoMantenimiento resultado = eleccion == 0
                ? ResultadoMantenimiento.REPARADO : ResultadoMantenimiento.NO_REPARABLE;
        String observaciones = JOptionPane.showInputDialog(this,
                "Observaciones del cierre (opcional):", "Finalizar mantenimiento", JOptionPane.QUESTION_MESSAGE);
        if (observaciones == null) {
            return; // canceló
        }
        ejecutarCambio(() -> controladorEstado.finalizarMantenimiento(idEquipoSeleccionado, resultado, observaciones),
                "El mantenimiento se finalizó correctamente.");
    }

    private void ejecutarCambio(Runnable accion, String mensajeExito) {
        try {
            accion.run();
            informar(mensajeExito);
            limpiarFormulario();
            refrescarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // ---------- Código QR del equipo ----------

    /** Guarda como PNG el QR que contiene el código de inventario del equipo seleccionado. */
    private void generarQr() {
        if (idEquipoSeleccionado == 0) {
            informar("Seleccione un equipo de la tabla.");
            return;
        }
        String codigo = textInventario.getText();

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar código QR de " + codigo);
        selector.setSelectedFile(new File(codigo + ".png"));
        selector.setFileFilter(new FileNameExtensionFilter("Imagen PNG", "png"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".png")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".png");
        }

        try {
            controladorQr.generarQr(codigo, destino);
            JLabel vistaPrevia = new JLabel("Guardado en: " + destino.getAbsolutePath(),
                    new ImageIcon(destino.getAbsolutePath()), SwingConstants.CENTER);
            vistaPrevia.setVerticalTextPosition(SwingConstants.BOTTOM);
            vistaPrevia.setHorizontalTextPosition(SwingConstants.CENTER);
            JOptionPane.showMessageDialog(this, vistaPrevia, "QR de " + codigo, JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void informar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Equipos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(this, ManejadorErrores.mensajeParaUsuario(e),
                "No se pudo completar la operación", JOptionPane.WARNING_MESSAGE);
    }
}
