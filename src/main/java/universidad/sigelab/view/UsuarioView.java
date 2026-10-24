package universidad.sigelab.view;

import universidad.sigelab.controller.ReconocimientoFacialController;
import universidad.sigelab.controller.UsuarioController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Rol;
import universidad.sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Administración de usuarios de SIGELAB.
 * Las reglas de negocio permanecen en UsuarioService.
 */
public class UsuarioView extends JFrame {

    // Componentes vinculados con UsuarioView.form
    private JPanel Panelprincipal;
    private JTable tablaUsuarios;
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
    private JButton btRegistrarRostro;

    // Paleta visual
    private static final Color AZUL_OSCURO =
            new Color(25, 45, 75);
    private static final Color AZUL =
            new Color(43, 108, 176);
    private static final Color FONDO =
            new Color(245, 247, 251);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO =
            new Color(45, 55, 72);
    private static final Color GRIS =
            new Color(220, 226, 235);
    private static final Color VERDE =
            new Color(22, 135, 95);
    private static final Color ROJO =
            new Color(194, 55, 55);
    private static final Color GRIS_BOTON =
            new Color(100, 116, 139);

    private static final String[] COLUMNAS = {
            "Usuario", "Nombre", "Apellido",
            "Correo", "Rol", "Estado"
    };

    private final UsuarioController controladorUsuario =
            new UsuarioController();

    private final ReconocimientoFacialController controladorFacial =
            new ReconocimientoFacialController();

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(COLUMNAS, 0) {
                @Override
                public boolean isCellEditable(
                        int fila, int columna) {
                    return false;
                }
            };

    private List<Usuario> usuariosMostrados =
            new ArrayList<>();

    // 0 = nuevo usuario; otro valor = edición
    private int idUsuarioSeleccionado = 0;

    public UsuarioView() {
        super("SIGELAB | Administración de usuarios");

        setContentPane(Panelprincipal);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        configurarTabla();
        aplicarEstiloVisual();
        cargarRoles();
        registrarEventos();
        refrescarTabla();

        setSize(1100, 700);
        setMinimumSize(new java.awt.Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    // =====================================================
    // CONFIGURACIÓN INICIAL
    // =====================================================

    private void configurarTabla() {
        tablaUsuarios.setModel(modeloTabla);

        tablaUsuarios.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tablaUsuarios.getSelectionModel()
                .addListSelectionListener(evento -> {
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
        btNuevo.addActionListener(
                evento -> limpiarFormulario()
        );

        btGuardar.addActionListener(
                evento -> guardar()
        );

        btDesactivar.addActionListener(
                evento -> cambiarEstado(false)
        );

        btActivar.addActionListener(
                evento -> cambiarEstado(true)
        );

        btRegistrarRostro.addActionListener(
                evento -> registrarRostro()
        );

        labelEstado.setText("-");
    }

    // =====================================================
    // DISEÑO VISUAL
    // =====================================================

    private void aplicarEstiloVisual() {
        getContentPane().setBackground(FONDO);

        if (Panelprincipal != null) {
            Panelprincipal.setBackground(FONDO);
            Panelprincipal.setBorder(
                    new EmptyBorder(16, 18, 16, 18)
            );
        }

        estilizarCampo(textNombre);
        estilizarCampo(textApellido);
        estilizarCampo(textNombreUsuario);
        estilizarCampo(textCorreo);
        estilizarCampo(textContrasena);
        estilizarCampo(textReferenciaFacial);

        estilizarCombo(comboRol);
        estilizarEstado(labelEstado);

        estilizarBoton(btNuevo, GRIS_BOTON);
        estilizarBoton(btGuardar, AZUL);
        estilizarBoton(btActivar, VERDE);
        estilizarBoton(btDesactivar, ROJO);
        estilizarBoton(btRegistrarRostro, AZUL_OSCURO);

        estilizarTabla(tablaUsuarios);

        // Los contenedores mantienen una apariencia uniforme.
        aplicarFondoContenedores(Panelprincipal);

        revalidate();
        repaint();
    }

    private void estilizarCampo(JTextField campo) {
        if (campo == null) {
            return;
        }

        campo.setFont(
                new Font("Segoe UI", Font.PLAIN, 14)
        );

        campo.setForeground(TEXTO);
        campo.setBackground(BLANCO);
        campo.setCaretColor(AZUL);

        campo.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(GRIS, 1, true),
                        new EmptyBorder(7, 10, 7, 10)
                )
        );

        campo.setPreferredSize(
                new java.awt.Dimension(180, 36)
        );
    }

    private void estilizarCombo(JComboBox<Rol> combo) {
        if (combo == null) {
            return;
        }

        combo.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        combo.setForeground(TEXTO);
        combo.setBackground(BLANCO);

        combo.setBorder(
                new LineBorder(GRIS, 1, true)
        );

        combo.setPreferredSize(
                new java.awt.Dimension(180, 36)
        );
    }

    private void estilizarEstado(JLabel etiqueta) {
        if (etiqueta == null) {
            return;
        }

        etiqueta.setFont(
                new Font("Segoe UI", Font.BOLD, 13)
        );

        etiqueta.setForeground(AZUL_OSCURO);
        etiqueta.setOpaque(true);
        etiqueta.setBackground(
                new Color(226, 232, 240)
        );

        etiqueta.setBorder(
                new EmptyBorder(6, 10, 6, 10)
        );
    }

    private void estilizarBoton(
            JButton boton, Color colorOriginal) {

        if (boton == null) {
            return;
        }

        boton.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        boton.setForeground(BLANCO);
        boton.setBackground(colorOriginal);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);

        boton.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        boton.setBorder(
                new EmptyBorder(9, 15, 9, 15)
        );

        boton.addMouseListener(
                new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(
                            java.awt.event.MouseEvent e) {
                        if (boton.isEnabled()) {
                            boton.setBackground(
                                    colorOriginal.darker()
                            );
                        }
                    }

                    @Override
                    public void mouseExited(
                            java.awt.event.MouseEvent e) {
                        boton.setBackground(colorOriginal);
                    }
                }
        );
    }

    private void estilizarTabla(JTable tabla) {
        if (tabla == null) {
            return;
        }

        tabla.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        tabla.setForeground(TEXTO);
        tabla.setBackground(BLANCO);
        tabla.setGridColor(
                new Color(232, 236, 242)
        );

        tabla.setRowHeight(34);
        tabla.setShowGrid(true);
        tabla.setShowVerticalLines(false);
        tabla.setIntercellSpacing(
                new java.awt.Dimension(0, 1)
        );

        tabla.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tabla.setSelectionBackground(
                new Color(219, 234, 254)
        );

        tabla.setSelectionForeground(AZUL_OSCURO);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);

        tabla.getTableHeader().setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        tabla.getTableHeader().setBackground(AZUL_OSCURO);
        tabla.getTableHeader().setForeground(BLANCO);
        tabla.getTableHeader().setOpaque(true);
        tabla.getTableHeader().setReorderingAllowed(false);

        tabla.getTableHeader().setPreferredSize(
                new java.awt.Dimension(0, 38)
        );

        DefaultTableCellRenderer renderizador =
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean isSelected,
                            boolean hasFocus,
                            int row,
                            int column) {

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
                            componente.setBackground(
                                    new Color(219, 234, 254)
                            );

                            componente.setForeground(
                                    AZUL_OSCURO
                            );

                        } else {
                            componente.setBackground(
                                    row % 2 == 0
                                            ? BLANCO
                                            : new Color(247, 249, 252)
                            );

                            componente.setForeground(TEXTO);
                        }

                        setBorder(
                                new EmptyBorder(4, 9, 4, 9)
                        );

                        return componente;
                    }
                };

        renderizador.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel()
                    .getColumn(i)
                    .setCellRenderer(renderizador);
        }

        if (tabla.getParent() instanceof JViewport) {
            tabla.getParent().setBackground(BLANCO);
        }
    }

    private void aplicarFondoContenedores(
            java.awt.Container contenedor) {

        if (contenedor == null) {
            return;
        }

        for (Component componente :
                contenedor.getComponents()) {

            if (componente instanceof JScrollPane) {
                JScrollPane scroll =
                        (JScrollPane) componente;

                scroll.setBorder(
                        new LineBorder(GRIS, 1, true)
                );

                scroll.getViewport().setBackground(BLANCO);

            } else if (componente instanceof JPanel) {
                componente.setBackground(FONDO);
            }

            if (componente instanceof java.awt.Container) {
                aplicarFondoContenedores(
                        (java.awt.Container) componente
                );
            }
        }
    }

    // =====================================================
    // TABLA Y SELECCIÓN
    // =====================================================

    private void refrescarTabla() {
        try {
            usuariosMostrados =
                    new ArrayList<>(controladorUsuario.listar());

            modeloTabla.setRowCount(0);

            for (Usuario usuario : usuariosMostrados) {
                modeloTabla.addRow(new Object[]{
                        usuario.getNombreUsuario(),
                        usuario.getNombre(),
                        usuario.getApellido(),
                        usuario.getCorreo(),
                        usuario.getRol().getNombre().name(),
                        usuario.getEstado().name()
                });
            }

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void alSeleccionarFila() {
        int fila = tablaUsuarios.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int filaModelo =
                tablaUsuarios.convertRowIndexToModel(fila);

        if (filaModelo < 0
                || filaModelo >= usuariosMostrados.size()) {
            return;
        }

        Usuario usuario = usuariosMostrados.get(filaModelo);

        idUsuarioSeleccionado = usuario.getIdUsuario();

        textNombre.setText(usuario.getNombre());
        textApellido.setText(usuario.getApellido());
        textNombreUsuario.setText(usuario.getNombreUsuario());
        textCorreo.setText(usuario.getCorreo());

        seleccionarRol(usuario.getRol().getIdRol());

        // Al editar, una contraseña vacía significa que no se escribe
        // una nueva contraseña, según la lógica del servicio.
        textContrasena.setText("");

        textReferenciaFacial.setText(
                usuario.getReferenciaFacial()
        );

        labelEstado.setText(
                usuario.getEstado().name()
        );
    }

    private void seleccionarRol(int idRol) {
        comboRol.setSelectedItem(null);

        for (int i = 0; i < comboRol.getItemCount(); i++) {
            Rol rol = comboRol.getItemAt(i);

            if (rol.getIdRol() == idRol) {
                comboRol.setSelectedIndex(i);
                return;
            }
        }
    }

    // =====================================================
    // NUEVO USUARIO
    // =====================================================

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

    // =====================================================
    // GUARDAR O MODIFICAR USUARIO
    // =====================================================

    private void guardar() {
        char[] caracteresContrasena =
                textContrasena.getPassword();

        String contrasena =
                new String(caracteresContrasena);

        Arrays.fill(caracteresContrasena, '\0');

        try {
            Rol rol = (Rol) comboRol.getSelectedItem();

            Usuario guardado;

            if (idUsuarioSeleccionado == 0) {
                guardado = controladorUsuario.registrar(
                        textNombre.getText(),
                        textApellido.getText(),
                        textNombreUsuario.getText(),
                        textCorreo.getText(),
                        rol,
                        contrasena,
                        textReferenciaFacial.getText()
                );

            } else {
                guardado = controladorUsuario.modificar(
                        idUsuarioSeleccionado,
                        textNombre.getText(),
                        textApellido.getText(),
                        textNombreUsuario.getText(),
                        textCorreo.getText(),
                        rol,
                        contrasena,
                        textReferenciaFacial.getText()
                );
            }

            JOptionPane.showMessageDialog(
                    this,
                    "El usuario "
                            + guardado.getNombreUsuario()
                            + " se guardó correctamente.",
                    "Usuario guardado",
                    JOptionPane.INFORMATION_MESSAGE
            );

            limpiarFormulario();
            refrescarTabla();

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // =====================================================
    // ACTIVAR Y DESACTIVAR USUARIO
    // =====================================================

    private void cambiarEstado(boolean activar) {
        if (idUsuarioSeleccionado == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione un usuario de la tabla.",
                    "Usuarios",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        String accion = activar ? "activar" : "desactivar";

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Desea " + accion + " al usuario "
                        + textNombreUsuario.getText() + "?",
                "Confirmar acción",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (activar) {
                controladorUsuario.activar(
                        idUsuarioSeleccionado
                );
            } else {
                controladorUsuario.desactivar(
                        idUsuarioSeleccionado
                );
            }

            limpiarFormulario();
            refrescarTabla();

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    // =====================================================
    // REGISTRO FACIAL
    // =====================================================

    private void registrarRostro() {
        if (idUsuarioSeleccionado == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione un usuario de la tabla.",
                    "Registrar rostro",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "Se abrirá la cámara y se tomarán varias fotos "
                        + "del rostro de "
                        + textNombreUsuario.getText()
                        + ".\nLa persona debe estar frente a la cámara."
                        + "\n¿Desea continuar?",
                "Registrar rostro",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        int idUsuario = idUsuarioSeleccionado;
        String nombreUsuario = textNombreUsuario.getText();

        btRegistrarRostro.setEnabled(false);

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                controladorFacial.registrarRostro(idUsuario);
                return null;
            }

            @Override
            protected void done() {
                btRegistrarRostro.setEnabled(true);

                try {
                    get();

                    JOptionPane.showMessageDialog(
                            UsuarioView.this,
                            "El rostro de " + nombreUsuario
                                    + " se registró correctamente.",
                            "Registro facial completado",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(
                            UsuarioView.this,
                            ManejadorErrores.mensajeParaUsuario(
                                    e.getCause()
                            ),
                            "No se pudo registrar el rostro",
                            JOptionPane.WARNING_MESSAGE
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    JOptionPane.showMessageDialog(
                            UsuarioView.this,
                            "La operación fue interrumpida.",
                            "Registro facial",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        }.execute();
    }

    // =====================================================
    // MANEJO DE ERRORES
    // =====================================================

    private void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(
                this,
                ManejadorErrores.mensajeParaUsuario(e),
                "No se pudo completar la operación",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
