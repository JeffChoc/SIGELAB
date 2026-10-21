package universidad.sigelab.view;

import universidad.sigelab.controller.UsuarioController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Rol;
import universidad.sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Pantalla de usuarios (solo ADMIN). Las reglas viven en UsuarioService. */
public class UsuarioView extends JFrame {

    // Estos nombres deben coincidir con el "field name" de cada componente en el diseñador (.form)
    private JPanel Panelprincipal;
    private JTable tablaUsuarios;              // dentro de un JScrollPane
    private JTextField textNombre;
    private JTextField textApellido;
    private JTextField textNombreUsuario;
    private JTextField textCorreo;
    private JComboBox<Rol> comboRol;
    private JPasswordField textContrasena;
    private JTextField textReferenciaFacial;
    private JLabel labelEstado;
    private JButton btNuevo;
    private JButton btGuardar;
    private JButton btDesactivar;
    private JButton btActivar;

    private static final String[] COLUMNAS = {"Usuario", "Nombre", "Apellido", "Correo", "Rol", "Estado"};

    private final UsuarioController controladorUsuario = new UsuarioController();

    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private List<Usuario> usuariosMostrados = new ArrayList<>();

    /** 0 = se está registrando uno nuevo; distinto de 0 = se está editando ese usuario. */
    private int idUsuarioSeleccionado = 0;

    public UsuarioView() {
        super("SIGELAB - Usuarios");
        setContentPane(Panelprincipal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarTabla();
        cargarRoles();
        registrarEventos();
        refrescarTabla();

        setSize(1000, 620);
        setLocationRelativeTo(null);
    }

    // ---------- Configuración inicial ----------

    private void configurarTabla() {
        tablaUsuarios.setModel(modeloTabla);
        tablaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaUsuarios.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                alSeleccionarFila();
            }
        });
    }

    private void cargarRoles() {
        try {
            comboRol.removeAllItems();
            for (Rol rol : controladorUsuario.rolesParaCombo()) {
                comboRol.addItem(rol);
            }
            comboRol.setSelectedItem(null);
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void registrarEventos() {
        btNuevo.addActionListener(evento -> limpiarFormulario());
        btGuardar.addActionListener(evento -> guardar());
        btDesactivar.addActionListener(evento -> cambiarEstado(false));
        btActivar.addActionListener(evento -> cambiarEstado(true));
        labelEstado.setText("-");
    }

    // ---------- Tabla ----------

    private void refrescarTabla() {
        try {
            usuariosMostrados = new ArrayList<>(controladorUsuario.listar());
            modeloTabla.setRowCount(0);
            for (Usuario usuario : usuariosMostrados) {
                modeloTabla.addRow(new Object[]{
                        usuario.getNombreUsuario(),
                        usuario.getNombre(),
                        usuario.getApellido(),
                        usuario.getCorreo(),
                        usuario.getRol().getNombre().name(),
                        usuario.getEstado().name()});
            }
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void alSeleccionarFila() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila < 0 || fila >= usuariosMostrados.size()) {
            return;
        }
        Usuario usuario = usuariosMostrados.get(tablaUsuarios.convertRowIndexToModel(fila));
        idUsuarioSeleccionado = usuario.getIdUsuario();
        textNombre.setText(usuario.getNombre());
        textApellido.setText(usuario.getApellido());
        textNombreUsuario.setText(usuario.getNombreUsuario());
        textCorreo.setText(usuario.getCorreo());
        seleccionarRol(usuario.getRol().getIdRol());
        textContrasena.setText("");
        textReferenciaFacial.setText(usuario.getReferenciaFacial());
        labelEstado.setText(usuario.getEstado().name());
    }

    private void seleccionarRol(int idRol) {
        comboRol.setSelectedItem(null);
        for (int i = 0; i < comboRol.getItemCount(); i++) {
            if (comboRol.getItemAt(i).getIdRol() == idRol) {
                comboRol.setSelectedIndex(i);
                return;
            }
        }
    }

    // ---------- Acciones ----------

    private void limpiarFormulario() {
        idUsuarioSeleccionado = 0;
        tablaUsuarios.clearSelection();
        textNombre.setText("");
        textApellido.setText("");
        textNombreUsuario.setText("");
        textCorreo.setText("");
        comboRol.setSelectedItem(null);
        textContrasena.setText("");
        textReferenciaFacial.setText("");
        labelEstado.setText("-");
        textNombre.requestFocusInWindow();
    }

    private void guardar() {
        char[] caracteresContrasena = textContrasena.getPassword();
        String contrasena = new String(caracteresContrasena);
        Arrays.fill(caracteresContrasena, '0');
        try {
            Rol rol = (Rol) comboRol.getSelectedItem();
            Usuario guardado;
            if (idUsuarioSeleccionado == 0) {
                guardado = controladorUsuario.registrar(textNombre.getText(), textApellido.getText(),
                        textNombreUsuario.getText(), textCorreo.getText(), rol, contrasena,
                        textReferenciaFacial.getText());
            } else {
                guardado = controladorUsuario.modificar(idUsuarioSeleccionado, textNombre.getText(),
                        textApellido.getText(), textNombreUsuario.getText(), textCorreo.getText(),
                        rol, contrasena, textReferenciaFacial.getText());
            }
            JOptionPane.showMessageDialog(this,
                    "El usuario " + guardado.getNombreUsuario() + " se guardó correctamente.",
                    "Usuarios", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            refrescarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void cambiarEstado(boolean activar) {
        if (idUsuarioSeleccionado == 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla.",
                    "Usuarios", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String accion = activar ? "activar" : "desactivar";
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Desea " + accion + " al usuario " + textNombreUsuario.getText() + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (activar) {
                controladorUsuario.activar(idUsuarioSeleccionado);
            } else {
                controladorUsuario.desactivar(idUsuarioSeleccionado);
            }
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