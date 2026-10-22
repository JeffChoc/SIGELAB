package universidad.sigelab.view;

import universidad.sigelab.controller.AutenticacionController;
import universidad.sigelab.exception.ManejadorErrores;
import universidad.sigelab.model.Usuario;

import javax.swing.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/** Solo captura y muestra datos: la lógica del login (credenciales + rostro) vive en AutenticacionService. */
public class LoginView extends JFrame {

    // Estos nombres deben coincidir con el "field name" de cada componente en el diseñador (.form)
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

        pack(); // si la ventana sale muy pequeña, use setSize(400, 300) en lugar de pack()
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void ingresar() {
        String nombreUsuario = textUsuario.getText();
        char[] caracteresContrasena = textPassword.getPassword();
        String contrasena = new String(caracteresContrasena);
        Arrays.fill(caracteresContrasena, '0');

        // La verificación facial abre la cámara y tarda unos segundos: se hace fuera del hilo de la
        // interfaz para que la ventana no se congele.
        String textoOriginal = btIngresar.getText();
        btIngresar.setEnabled(false);
        btIngresar.setText("Verificando rostro...");

        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() {
                return controladorAutenticacion.iniciarSesion(nombreUsuario, contrasena);
            }

            @Override
            protected void done() {
                btIngresar.setEnabled(true);
                btIngresar.setText(textoOriginal);
                try {
                    Usuario usuario = get();
                    new PrincipalFrame(usuario).setVisible(true);
                    dispose();
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(LoginView.this, ManejadorErrores.mensajeParaUsuario(e.getCause()),
                            "No se pudo iniciar sesión", JOptionPane.WARNING_MESSAGE);
                    textPassword.setText("");
                    textPassword.requestFocusInWindow();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
