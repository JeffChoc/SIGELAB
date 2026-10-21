package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.Laboratorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LaboratorioRepository {

    private static final String SELECT_BASE =
            "SELECT id_laboratorio, codigo, nombre, ubicacion, capacidad, estado FROM Laboratorio";

    public List<Laboratorio> listarTodos() {
        List<Laboratorio> laboratorios = new ArrayList<>();
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_BASE + " ORDER BY codigo");
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                laboratorios.add(mapear(resultado));
            }
            return laboratorios;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los laboratorios", e);
        }
    }

    public Optional<Laboratorio> buscarPorId(int idLaboratorio) {
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_BASE + " WHERE id_laboratorio = ?")) {
            sentencia.setInt(1, idLaboratorio);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el laboratorio " + idLaboratorio, e);
        }
    }

    /** idExcluido = 0 al registrar; el id propio al modificar. */
    public boolean existeCodigo(String codigo, int idExcluido) {
        String sql = "SELECT COUNT(*) FROM Laboratorio WHERE codigo = ? AND id_laboratorio <> ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, codigo);
            sentencia.setInt(2, idExcluido);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() && resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al verificar el código de laboratorio", e);
        }
    }

    public int insertar(Laboratorio laboratorio) {
        String sql = "INSERT INTO Laboratorio (codigo, nombre, ubicacion, capacidad, estado) VALUES (?, ?, ?, ?, ?)";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, laboratorio.getCodigo());
            sentencia.setString(2, laboratorio.getNombre());
            sentencia.setString(3, laboratorio.getUbicacion());
            sentencia.setInt(4, laboratorio.getCapacidad());
            sentencia.setString(5, laboratorio.getEstado().name());
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
                throw new DataAccessException("No se obtuvo el id generado del laboratorio", null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar el laboratorio " + laboratorio.getCodigo(), e);
        }
    }

    public void actualizar(Laboratorio laboratorio) {
        String sql = "UPDATE Laboratorio SET codigo = ?, nombre = ?, ubicacion = ?, capacidad = ? WHERE id_laboratorio = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, laboratorio.getCodigo());
            sentencia.setString(2, laboratorio.getNombre());
            sentencia.setString(3, laboratorio.getUbicacion());
            sentencia.setInt(4, laboratorio.getCapacidad());
            sentencia.setInt(5, laboratorio.getIdLaboratorio());
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el laboratorio " + laboratorio.getIdLaboratorio(), e);
        }
    }

    public void actualizarEstado(int idLaboratorio, EstadoRegistro estado) {
        String sql = "UPDATE Laboratorio SET estado = ? WHERE id_laboratorio = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, estado.name());
            sentencia.setInt(2, idLaboratorio);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar el estado del laboratorio " + idLaboratorio, e);
        }
    }

    private Laboratorio mapear(ResultSet resultado) throws SQLException {
        return new Laboratorio(
                resultado.getInt("id_laboratorio"),
                resultado.getString("codigo"),
                resultado.getString("nombre"),
                resultado.getString("ubicacion"),
                resultado.getInt("capacidad"),
                EstadoRegistro.valueOf(resultado.getString("estado")));
    }
}
