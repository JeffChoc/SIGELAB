package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.DashboardResumen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardRepository {

    /** Lee la vista vw_dashboard, que calcula todos los contadores en una sola fila. */
    public DashboardResumen obtener() {
        String sql = "SELECT total_equipos, disponibles, prestados, danados, en_mantenimiento, "
                + "de_baja, prestamos_activos, prestamos_vencidos FROM vw_dashboard";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            if (!resultado.next()) {
                return new DashboardResumen(0, 0, 0, 0, 0, 0, 0, 0);
            }
            return new DashboardResumen(
                    resultado.getInt("total_equipos"),
                    resultado.getInt("disponibles"),
                    resultado.getInt("prestados"),
                    resultado.getInt("danados"),
                    resultado.getInt("en_mantenimiento"),
                    resultado.getInt("de_baja"),
                    resultado.getInt("prestamos_activos"),
                    resultado.getInt("prestamos_vencidos"));
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar el dashboard", e);
        }
    }
}
