package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RolRepository {

    public List<Rol> listarTodos() {
        String sql = "SELECT id_rol, nombre, descripcion FROM Rol ORDER BY id_rol";
        List<Rol> roles = new ArrayList<>();
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                roles.add(mapear(resultado));
            }
            return roles;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los roles", e);
        }
    }

    public Optional<Rol> buscarPorId(int idRol) {
        String sql = "SELECT id_rol, nombre, descripcion FROM Rol WHERE id_rol = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idRol);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el rol " + idRol, e);
        }
    }

    private Rol mapear(ResultSet resultado) throws SQLException {
        try {
            return new Rol(
                    resultado.getInt("id_rol"),
                    RolUsuario.desde(resultado.getString("nombre")),
                    resultado.getString("descripcion"));
        } catch (IllegalArgumentException e) {
            throw new DataAccessException("La base contiene un rol no reconocido: "
                    + resultado.getString("nombre"), e);
        }
    }
}
