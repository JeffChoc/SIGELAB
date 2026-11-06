package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.enums.EstadoPrestamo;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.Prestamo;
import universidad.sigelab.model.PrestamoResumen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrestamoRepository {

    /** Devuelve el id generado. Dentro de una transacción ajena: recibe la Connection. */
    public int insertar(Connection conexion, Prestamo prestamo) {
        String sql = "INSERT INTO Prestamo (solicitante_nombre, solicitante_identificacion, "
                + "fecha_esperada_devolucion, id_usuario_registra, observaciones) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, prestamo.getSolicitanteNombre());
            sentencia.setString(2, prestamo.getSolicitanteIdentificacion());
            sentencia.setTimestamp(3, Timestamp.valueOf(prestamo.getFechaEsperadaDevolucion()));
            sentencia.setInt(4, prestamo.getIdUsuarioRegistra());
            sentencia.setString(5, prestamo.getObservaciones());
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
                throw new DataAccessException("No se obtuvo el id generado del préstamo", null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar el préstamo", e);
        }
    }

    public void insertarDetalle(Connection conexion, int idPrestamo, int idEquipo) {
        String sql = "INSERT INTO DetallePrestamo (id_prestamo, id_equipo) VALUES (?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idPrestamo);
            sentencia.setInt(2, idEquipo);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar el detalle del préstamo " + idPrestamo, e);
        }
    }

    public Optional<Prestamo> buscarPorId(int idPrestamo) {
        String sql = "SELECT id_prestamo, solicitante_nombre, solicitante_identificacion, fecha_prestamo, "
                + "fecha_esperada_devolucion, id_usuario_registra, estado, observaciones "
                + "FROM Prestamo WHERE id_prestamo = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idPrestamo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return Optional.empty();
                }
                Prestamo prestamo = new Prestamo();
                prestamo.setIdPrestamo(resultado.getInt("id_prestamo"));
                prestamo.setSolicitanteNombre(resultado.getString("solicitante_nombre"));
                prestamo.setSolicitanteIdentificacion(resultado.getString("solicitante_identificacion"));
                prestamo.setFechaPrestamo(resultado.getTimestamp("fecha_prestamo").toLocalDateTime());
                prestamo.setFechaEsperadaDevolucion(
                        resultado.getTimestamp("fecha_esperada_devolucion").toLocalDateTime());
                prestamo.setIdUsuarioRegistra(resultado.getInt("id_usuario_registra"));
                prestamo.setEstado(EstadoPrestamo.valueOf(resultado.getString("estado")));
                prestamo.setObservaciones(resultado.getString("observaciones"));
                return Optional.of(prestamo);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el préstamo " + idPrestamo, e);
        }
    }

    /** RN-16: se llama solo cuando ya no quedan equipos pendientes. Solo actúa si sigue ACTIVO. */
    public boolean finalizar(Connection conexion, int idPrestamo) {
        String sql = "UPDATE Prestamo SET estado = 'FINALIZADO' WHERE id_prestamo = ? AND estado = 'ACTIVO'";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idPrestamo);
            return sentencia.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Error al finalizar el préstamo " + idPrestamo, e);
        }
    }

    /** Usa la vista vw_prestamos_con_pendientes (JOIN + COUNT/SUM + GROUP BY). */
    public List<PrestamoResumen> listarResumen() {
        String sql = "SELECT id_prestamo, solicitante_nombre, fecha_prestamo, fecha_esperada_devolucion, "
                + "estado, total_equipos, equipos_pendientes "
                + "FROM vw_prestamos_con_pendientes ORDER BY fecha_prestamo DESC";
        List<PrestamoResumen> resumenes = new ArrayList<>();
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                resumenes.add(new PrestamoResumen(
                        resultado.getInt("id_prestamo"),
                        resultado.getString("solicitante_nombre"),
                        resultado.getTimestamp("fecha_prestamo").toLocalDateTime(),
                        resultado.getTimestamp("fecha_esperada_devolucion").toLocalDateTime(),
                        EstadoPrestamo.valueOf(resultado.getString("estado")),
                        resultado.getInt("total_equipos"),
                        resultado.getInt("equipos_pendientes")));
            }
            return resumenes;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los préstamos", e);
        }
    }
}
