package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.TipoEquipo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TipoEquipoRepository {

    public List<TipoEquipo> listarTodos() {
        String sql = "SELECT id_tipo_equipo, nombre, descripcion FROM TipoEquipo ORDER BY nombre";
        List<TipoEquipo> tipos = new ArrayList<>();
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                tipos.add(mapear(resultado));
            }
            return tipos;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los tipos de equipo", e);
        }
    }

    public Optional<TipoEquipo> buscarPorId(int idTipoEquipo) {
        String sql = "SELECT id_tipo_equipo, nombre, descripcion FROM TipoEquipo WHERE id_tipo_equipo = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idTipoEquipo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el tipo de equipo " + idTipoEquipo, e);
        }
    }

    public boolean existeNombre(String nombre, int idExcluido) {
        String sql = "SELECT COUNT(*) FROM TipoEquipo WHERE nombre = ? AND id_tipo_equipo <> ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nombre);
            sentencia.setInt(2, idExcluido);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() && resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al verificar el nombre del tipo de equipo", e);
        }
    }

    public int insertar(TipoEquipo tipo) {
        String sql = "INSERT INTO TipoEquipo (nombre, descripcion) VALUES (?, ?)";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, tipo.getNombre());
            sentencia.setString(2, tipo.getDescripcion());
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
                throw new DataAccessException("No se obtuvo el id generado del tipo de equipo", null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar el tipo de equipo " + tipo.getNombre(), e);
        }
    }

    public void actualizar(TipoEquipo tipo) {
        String sql = "UPDATE TipoEquipo SET nombre = ?, descripcion = ? WHERE id_tipo_equipo = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, tipo.getNombre());
            sentencia.setString(2, tipo.getDescripcion());
            sentencia.setInt(3, tipo.getIdTipoEquipo());
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el tipo de equipo " + tipo.getIdTipoEquipo(), e);
        }
    }

    private TipoEquipo mapear(ResultSet resultado) throws SQLException {
        return new TipoEquipo(
                resultado.getInt("id_tipo_equipo"),
                resultado.getString("nombre"),
                resultado.getString("descripcion"));
    }
}
