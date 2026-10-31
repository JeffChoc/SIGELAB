package universidad.sigelab;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.view.LoginView;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.sql.Connection;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // si falla, Swing usa su apariencia por defecto
        }

        if (!probarConexion()) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo conectar con la base de datos. Verifique que SQL Server esté en ejecución.",
                    "SIGELAB", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));
    }

    private static boolean probarConexion() {
        try (Connection conexion = DatabaseConnection.getConnection()) {
            System.out.println("SIGELAB: conexión a SQL Server exitosa.");
            return true;
        } catch (Exception e) {
            System.out.println("SIGELAB: error al conectar con la base de datos.");
            e.printStackTrace();
            return false;
        }
    }
}