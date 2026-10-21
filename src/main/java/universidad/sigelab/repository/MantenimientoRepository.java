package universidad.sigelab.repository;

import universidad.sigelab.enums.EstadoMantenimiento;
import universidad.sigelab.enums.ResultadoMantenimiento;
import universidad.sigelab.enums.TipoMantenimiento;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.Mantenimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

/** Todos los métodos reciben la Connection para poder participar en una transacción. */
public class MantenimientoRepository {

    /** Devuelve el id generado. La fecha de ingreso y el estado EN_PROCESO los pone la BD. */
    public int insertar(Connection conexion, Mantenimiento mantenimiento) {
        String sql = "INSERT INTO Mantenimiento (id_equipo, tipo, descripcion, responsable, observaciones, "
                + "id_usuario_registra) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, mantenimiento.getIdEquipo());
            sentencia.setString(2, mantenimiento.getTipo().name());
            sentencia.setString(3, mantenimiento.getDescripcion());
            sentencia.setString(4, mantenimiento.getResponsable());
            sentencia.setString(5, mantenimiento.getObservaciones());
            sentencia.setInt(6, mantenimiento.getIdUsuarioRegistra());
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
                throw new DataAccessException("No se obtuvo el id generado del mantenimiento", null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar el mantenimiento del equipo "
                    + mantenimiento.getIdEquipo(), e);
        }
    }

    public Optional<Mantenimiento> buscarAbiertoPorEquipo(Connection conexion, int idEquipo) {
        String sql = "SELECT id_mantenimiento, id_equipo, fecha_ingreso, tipo, descripcion, responsable, "
                + "fecha_salida, resultado, estado, observaciones, id_usuario_registra "
                + "FROM Mantenimiento WHERE id_equipo = ? AND estado = 'EN_PROCESO'";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idEquipo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el mantenimiento abierto del equipo " + idEquipo, e);
        }
    }

    /**
     * Cierra el mantenimiento. Solo actúa si sigue EN_PROCESO; devuelve false si ya estaba cerrado.
     * observaciones null = conservar las que ya tenía.
     */
    public boolean finalizar(Connection conexion, int idMantenimiento,
                             ResultadoMantenimiento resultado, String observaciones) {
        String sql = "UPDATE Mantenimiento SET estado = 'FINALIZADO', fecha_salida = SYSDATETIME(), "
                + "resultado = ?, observaciones = COALESCE(?, observaciones) "
                + "WHERE id_mantenimiento = ? AND estado = 'EN_PROCESO'";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, resultado.name());
            sentencia.setString(2, observaciones);
            sentencia.setInt(3, idMantenimiento);
            return sentencia.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Error al finalizar el mantenimiento " + idMantenimiento, e);
        }
    }

    private Mantenimiento mapear(ResultSet resultado) throws SQLException {
        Mantenimiento mantenimiento = new Mantenimiento();
        mantenimiento.setIdMantenimiento(resultado.getInt("id_mantenimiento"));
        mantenimiento.setIdEquipo(resultado.getInt("id_equipo"));
        Timestamp ingreso = resultado.getTimestamp("fecha_ingreso");
        mantenimiento.setFechaIngreso(ingreso == null ? null : ingreso.toLocalDateTime());
        mantenimiento.setTipo(TipoMantenimiento.valueOf(resultado.getString("tipo")));
        mantenimiento.setDescripcion(resultado.getString("descripcion"));
        mantenimiento.setResponsable(resultado.getString("responsable"));
        Timestamp salida = resultado.getTimestamp("fecha_salida");
        mantenimiento.setFechaSalida(salida == null ? null : salida.toLocalDateTime());
        String textoResultado = resultado.getString("resultado");
        mantenimiento.setResultado(textoResultado == null ? null : ResultadoMantenimiento.valueOf(textoResultado));
        mantenimiento.setEstado(EstadoMantenimiento.valueOf(resultado.getString("estado")));
        mantenimiento.setObservaciones(resultado.getString("observaciones"));
        mantenimiento.setIdUsuarioRegistra(resultado.getInt("id_usuario_registra"));
        return mantenimiento;
    }
}
