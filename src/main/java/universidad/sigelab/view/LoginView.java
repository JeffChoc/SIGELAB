package universidad.sigelab.view;

import universidad.sigelab.controller.AutenticacionController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Usuario;

import javax.swing.*;
import java.util.Arrays;


public class LoginView extends JFrame {

    private JPanel Panelprincipal;
    private JPasswordField textPassword;
    private JTextField textUsuario;
    private JButton btIngresar;

    private final AutenticacionController controladorAutenticacion = new AutenticacionController();

    public LoginView() {
        super("SIGELAB - Inicio de sesión");
        setContentPane(Panelprincipal);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        btIngresar.addActionListener(evento -> ingresar());
        getRootPane().setDefaultButton(btIngresar); // Enter equivale a pulsar Ingresar

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void ingresar() {
        String nombreUsuario = textUsuario.getText();
        char[] caracteresContrasena = textPassword.getPassword();
        String contrasena = new String(caracteresContrasena);
        Arrays.fill(caracteresContrasena, '0');

        try {
            // TEMPORAL: sin validación facial hasta la Etapa 4.
            Usuario usuario = controladorAutenticacion.iniciarSesionSinFacial(nombreUsuario, contrasena);
            new PrincipalFrame(usuario).setVisible(true);
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, ManejadorErrores.mensajeParaUsuario(e),
                    "No se pudo iniciar sesión", JOptionPane.WARNING_MESSAGE);
            textPassword.setText("");
            textPassword.requestFocusInWindow();
        }
    }
}