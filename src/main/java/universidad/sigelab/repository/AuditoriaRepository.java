package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.Auditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AuditoriaRepository {

    /** Inserta usando su propia conexión. */
    public void insertar(Auditoria auditoria) {
        try (Connection conexion = DatabaseConnection.getConnection()) {
            insertar(conexion, auditoria);
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar la auditoría", e);
        }
    }

    /** Inserta dentro de una transacción ajena (por ejemplo, la confirmación de un préstamo). */
    public void insertar(Connection conexion, Auditoria auditoria) {
        String sql = """
                INSERT INTO Auditoria (id_usuario, accion, entidad, id_registro, descripcion)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (auditoria.getIdUsuario() == null) {
                sentencia.setNull(1, Types.INTEGER);
            } else {
                sentencia.setInt(1, auditoria.getIdUsuario());
            }
            sentencia.setString(2, auditoria.getAccion().name());
            sentencia.setString(3, auditoria.getEntidad());
            if (auditoria.getIdRegistro() == null) {
                sentencia.setNull(4, Types.INTEGER);
            } else {
                sentencia.setInt(4, auditoria.getIdRegistro());
            }
            sentencia.setString(5, auditoria.getDescripcion());
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar la auditoría " + auditoria.getAccion(), e);
        }
    }
}
