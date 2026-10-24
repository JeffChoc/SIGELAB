
package universidad.sigelab.view;

import universidad.sigelab.controller.ReconocimientoFacialController;
import universidad.sigelab.controller.UsuarioController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Rol;
import universidad.sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    // Colores
    private static final Color AZUL_OSCURO =
            new Color(25, 45, 75);

    private static final Color AZUL =
            new Color(41, 98, 160);

    private static final Color VERDE =
            new Color(39, 140, 90);

    private static final Color ROJO =
            new Color(190, 55, 55);

    private static final Color FONDO =
            new Color(245, 247, 250);

    private static final Color BLANCO =
            Color.WHITE;

    private static final Color NEGRO =
            Color.BLACK;

    private static final Color BORDE =
            new Color(210, 218, 228);

    private static final String[] COLUMNAS = {
            "Usuario",
            "Nombre",
            "Apellido",
            "Correo",
            "Rol",
            "Estado"
    };

    private final UsuarioController controladorUsuario =
            new UsuarioController();

    private final ReconocimientoFacialController controladorFacial =
            new ReconocimientoFacialController();

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(COLUMNAS, 0) {

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private List<Usuario> usuariosMostrados =
            new ArrayList<>();

    // 0 = nuevo usuario; otro valor = usuario seleccionado.
    private int idUsuarioSeleccionado = 0;

    public UsuarioView() {
        super("SIGELAB | Administración de usuarios");

        setContentPane(Panelprincipal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarInterfaz();
        configurarTabla();
        cargarRoles();
        registrarEventos();
        refrescarTabla();

        setSize(1060, 680);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
    }

    // =====================================================
    // CONFIGURACIÓN DE LA INTERFAZ
    // =====================================================

    private void configurarInterfaz() {
        getContentPane().setBackground(FONDO);

        if (Panelprincipal != null) {
            Panelprincipal.setBackground(FONDO);
        }

        configurarCampo(textNombre);
        configurarCampo(textApellido);
        configurarCampo(textNombreUsuario);
        configurarCampo(textCorreo);
        configurarCampo(textContrasena);
        configurarCampo(textReferenciaFacial);

        configurarCombo(comboRol);

        if (labelEstado != null) {
            labelEstado.setForeground(NEGRO);
            labelEstado.setFont(
                    new Font("Segoe UI", Font.BOLD, 13)
            );
        }

        // Colores sólidos para los botones.
        configurarBoton(btNuevo, AZUL_OSCURO);
        configurarBoton(btGuardar, VERDE);
        configurarBoton(btActivar, VERDE);
        configurarBoton(btDesactivar, ROJO);
        configurarBoton(btRegistrarRostro, AZUL);
    }

    private void configurarCampo(JTextField campo) {
        if (campo == null) {
            return;
        }

        campo.setForeground(NEGRO);
        campo.setCaretColor(NEGRO);
        campo.setBackground(BLANCO);
        campo.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        campo.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        BorderFactory.createEmptyBorder(
                                7, 9, 7, 9
                        )
                )
        );
    }

    private void configurarCombo(JComboBox<Rol> combo) {
        if (combo == null) {
            return;
        }

        combo.setForeground(NEGRO);
        combo.setBackground(BLANCO);
        combo.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> lista,
                    Object valor,
                    int indice,
                    boolean seleccionado,
                    boolean tieneFoco) {

                super.getListCellRendererComponent(
                        lista,
                        valor,
                        indice,
                        seleccionado,
                        tieneFoco
                );

                setForeground(NEGRO);

                if (seleccionado) {
                    setBackground(
                            new Color(220, 232, 246)
                    );
                } else {
                    setBackground(BLANCO);
                }

                return this;
            }
        });
    }

    /**
     * Configura el botón con fondo sólido y texto blanco.
     * BasicButtonUI evita que el tema visual de Swing
     * ignore el color de fondo establecido.
     */
    private void configurarBoton(
            JButton boton,
            Color color) {

        if (boton == null) {
            return;
        }

        // Aplicar la interfaz básica antes de personalizar.
        boton.setUI(new BasicButtonUI());

        boton.setBackground(color);
        boton.setForeground(BLANCO);

        boton.setFont(
                new Font("Segoe UI", Font.BOLD, 13)
        );

        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setRolloverEnabled(false);

        boton.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        boton.setBorder(
                BorderFactory.createEmptyBorder(
                        10, 18, 10, 18
                )
        );

        boton.setMargin(
                new Insets(10, 18, 10, 18)
        );
    }

    // =====================================================
    // CONFIGURACIÓN DE LA TABLA
    // =====================================================

    private void configurarTabla() {
        tablaUsuarios.setModel(modeloTabla);

        tablaUsuarios.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tablaUsuarios.setRowHeight(34);

        tablaUsuarios.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        tablaUsuarios.setForeground(NEGRO);
        tablaUsuarios.setBackground(BLANCO);

        tablaUsuarios.setSelectionForeground(NEGRO);
        tablaUsuarios.setSelectionBackground(
                new Color(220, 232, 246)
        );

        tablaUsuarios.setGridColor(
                new Color(225, 231, 239)
        );

        tablaUsuarios.setShowHorizontalLines(true);
        tablaUsuarios.setShowVerticalLines(false);
        tablaUsuarios.setIntercellSpacing(
                new Dimension(0, 1)
        );

        tablaUsuarios.setFillsViewportHeight(true);
        tablaUsuarios.setAutoCreateRowSorter(true);

        // Texto negro en todas las celdas.
        DefaultTableCellRenderer renderizador =
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable tabla,
                            Object valor,
                            boolean seleccionado,
                            boolean tieneFoco,
                            int fila,
                            int columna) {

                        super.getTableCellRendererComponent(
                                tabla,
                                valor,
                                seleccionado,
                                tieneFoco,
                                fila,
                                columna
                        );

                        setForeground(NEGRO);

                        if (seleccionado) {
                            setBackground(
                                    new Color(220, 232, 246)
                            );
                        } else if (fila % 2 == 0) {
                            setBackground(BLANCO);
                        } else {
                            setBackground(
                                    new Color(246, 248, 251)
                            );
                        }

                        setBorder(
                                BorderFactory.createEmptyBorder(
                                        0, 10, 0, 10
                                )
                        );

                        setFont(
                                new Font("Segoe UI", Font.PLAIN, 12)
                        );

                        return this;
                    }
                };

        for (int columna = 0;
             columna < tablaUsuarios.getColumnCount();
             columna++) {

            tablaUsuarios.getColumnModel()
                    .getColumn(columna)
                    .setCellRenderer(renderizador);
        }

        // Encabezado con letras negras.
        JTableHeader encabezado =
                tablaUsuarios.getTableHeader();

        encabezado.setForeground(NEGRO);
        encabezado.setBackground(BLANCO);

        encabezado.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        encabezado.setOpaque(true);
        encabezado.setReorderingAllowed(false);

        encabezado.setPreferredSize(
                new Dimension(0, 38)
        );

        encabezado.setDefaultRenderer(
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable tabla,
                            Object valor,
                            boolean seleccionado,
                            boolean tieneFoco,
                            int fila,
                            int columna) {

                        super.getTableCellRendererComponent(
                                tabla,
                                valor,
                                seleccionado,
                                tieneFoco,
                                fila,
                                columna
                        );

                        setForeground(NEGRO);
                        setBackground(BLANCO);

                        setFont(
                                new Font("Segoe UI", Font.BOLD, 12)
                        );

                        setHorizontalAlignment(LEFT);

                        setBorder(
                                BorderFactory.createCompoundBorder(
                                        BorderFactory.createMatteBorder(
                                                0, 0, 1, 1, BORDE
                                        ),
                                        BorderFactory.createEmptyBorder(
                                                0, 10, 0, 10
                                        )
                                )
                        );

                        return this;
                    }
                });

        tablaUsuarios.getSelectionModel()
                .addListSelectionListener(evento -> {
                    if (!evento.getValueIsAdjusting()) {
                        alSeleccionarFila();
                    }
                });
    }

    // =====================================================
    // CARGAR ROLES Y USUARIOS
    // =====================================================

    private void cargarRoles() {
        try {
            comboRol.removeAllItems();

            for (Rol rol :
                    controladorUsuario.rolesParaCombo()) {
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
        labelEstado.setForeground(NEGRO);
    }

    private void refrescarTabla() {
        try {
            usuariosMostrados = new ArrayList<>(
                    controladorUsuario.listar()
            );

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

    // =====================================================
    // SELECCIONAR USUARIO
    // =====================================================

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

        Usuario usuario =
                usuariosMostrados.get(filaModelo);

        idUsuarioSeleccionado = usuario.getIdUsuario();

        textNombre.setText(usuario.getNombre());
        textApellido.setText(usuario.getApellido());

        textNombreUsuario.setText(
                usuario.getNombreUsuario()
        );

        textCorreo.setText(usuario.getCorreo());

        seleccionarRol(
                usuario.getRol().getIdRol()
        );

        textContrasena.setText("");

        textReferenciaFacial.setText(
                usuario.getReferenciaFacial()
        );

        labelEstado.setText(
                usuario.getEstado().name()
        );

        labelEstado.setForeground(NEGRO);
    }

    private void seleccionarRol(int idRol) {
        comboRol.setSelectedItem(null);

        for (int i = 0;
             i < comboRol.getItemCount();
             i++) {

            Rol rol = comboRol.getItemAt(i);

            if (rol.getIdRol() == idRol) {
                comboRol.setSelectedIndex(i);
                return;
            }
        }
    }

    // =====================================================
    // LIMPIAR FORMULARIO
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
        labelEstado.setForeground(NEGRO);

        textNombre.requestFocusInWindow();
    }

    // =====================================================
    // GUARDAR Y MODIFICAR USUARIOS
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
                    "Usuarios",
                    JOptionPane.INFORMATION_MESSAGE
            );

            limpiarFormulario();
            refrescarTabla();

        } catch (Exception e) {
            mostrarError(e);

        } finally {
            Arrays.fill(caracteresContrasena, '\0');
        }
    }

    // =====================================================
    // ACTIVAR Y DESACTIVAR USUARIOS
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

        String accion =
                activar ? "activar" : "desactivar";

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Desea " + accion + " al usuario "
                        + textNombreUsuario.getText() + "?",
                "Confirmar operación",
                JOptionPane.YES_NO_OPTION
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
    // REGISTRAR ROSTRO
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
                        + ".\nLa persona debe estar frente "
                        + "a la cámara. ¿Desea continuar?",
                "Registrar rostro",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        int idUsuario = idUsuarioSeleccionado;

        String nombreUsuario =
                textNombreUsuario.getText();

        btRegistrarRostro.setEnabled(false);

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                controladorFacial.registrarRostro(
                        idUsuario
                );

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
                            "Registrar rostro",
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
                            "Registrar rostro",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        }.execute();
    }

    // =====================================================
    // MOSTRAR ERRORES
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
