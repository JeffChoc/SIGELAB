package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.enums.CondicionDevolucion;
import universidad.sigelab.enums.EstadoEquipo;
import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.DetallePrestamo;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.Laboratorio;
import universidad.sigelab.model.TipoEquipo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un detalle SIN fila en Devolucion es un equipo pendiente (RN-10, RN-16).
 * UNIQUE(id_detalle) en la tabla impide devolver dos veces el mismo equipo (RN-11).
 */
public class DevolucionRepository {

    public void insertar(Connection conexion, int idDetalle, CondicionDevolucion condicion,
                         String observaciones, int idUsuarioRecibe) {
        String sql = "INSERT INTO Devolucion (id_detalle, condicion, observaciones, id_usuario_recibe) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idDetalle);
            sentencia.setString(2, condicion.name());
            sentencia.setString(3, observaciones);
            sentencia.setInt(4, idUsuarioRecibe);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al registrar la devolución del detalle " + idDetalle, e);
        }
    }

    /** Equipos del préstamo que todavía no se han devuelto. */
    public List<DetallePrestamo> listarPendientes(int idPrestamo) {
        String sql = "SELECT d.id_detalle, d.id_prestamo, e.id_equipo, e.codigo_inventario, e.nombre, "
                + "e.marca, e.modelo, e.numero_serie, e.estado, e.observaciones, "
                + "t.id_tipo_equipo, t.nombre AS tipo_nombre, t.descripcion AS tipo_descripcion, "
                + "l.id_laboratorio, l.codigo AS lab_codigo, l.nombre AS lab_nombre, "
                + "l.ubicacion AS lab_ubicacion, l.capacidad AS lab_capacidad, l.estado AS lab_estado "
                + "FROM DetallePrestamo d "
                + "JOIN Equipo e ON e.id_equipo = d.id_equipo "
                + "JOIN TipoEquipo t ON t.id_tipo_equipo = e.id_tipo_equipo "
                + "JOIN Laboratorio l ON l.id_laboratorio = e.id_laboratorio "
                + "LEFT JOIN Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE d.id_prestamo = ? AND dv.id_devolucion IS NULL "
                + "ORDER BY e.codigo_inventario";
        List<DetallePrestamo> pendientes = new ArrayList<>();
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idPrestamo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    pendientes.add(mapear(resultado));
                }
            }
            return pendientes;
        } catch (SQLException e) {
            throw new DataAccessException("Error al listar los equipos pendientes del préstamo " + idPrestamo, e);
        }
    }

    /** Cuenta pendientes dentro de la transacción en curso (ve las devoluciones recién insertadas). */
    public int contarPendientes(Connection conexion, int idPrestamo) {
        String sql = "SELECT COUNT(*) FROM DetallePrestamo d "
                + "LEFT JOIN Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE d.id_prestamo = ? AND dv.id_devolucion IS NULL";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idPrestamo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al contar los pendientes del préstamo " + idPrestamo, e);
        }
    }

    /** Préstamo ACTIVO donde el equipo figura pendiente de devolución. */
    public Optional<Integer> buscarPrestamoPendientePorCodigo(String codigoInventario) {
        String sql = "SELECT TOP 1 d.id_prestamo FROM DetallePrestamo d "
                + "JOIN Equipo e ON e.id_equipo = d.id_equipo "
                + "JOIN Prestamo p ON p.id_prestamo = d.id_prestamo "
                + "LEFT JOIN Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE e.codigo_inventario = ? AND dv.id_devolucion IS NULL AND p.estado = 'ACTIVO'";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, codigoInventario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(resultado.getInt(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar el préstamo del equipo " + codigoInventario, e);
        }
    }

    private DetallePrestamo mapear(ResultSet resultado) throws SQLException {
        TipoEquipo tipo = new TipoEquipo(
                resultado.getInt("id_tipo_equipo"),
                resultado.getString("tipo_nombre"),
                resultado.getString("tipo_descripcion"));
        Laboratorio laboratorio = new Laboratorio(
                resultado.getInt("id_laboratorio"),
                resultado.getString("lab_codigo"),
                resultado.getString("lab_nombre"),
                resultado.getString("lab_ubicacion"),
                resultado.getInt("lab_capacidad"),
                EstadoRegistro.valueOf(resultado.getString("lab_estado")));
        Equipo equipo = new Equipo(
                resultado.getInt("id_equipo"),
                resultado.getString("codigo_inventario"),
                resultado.getString("nombre"),
                tipo,
                resultado.getString("marca"),
                resultado.getString("modelo"),
                resultado.getString("numero_serie"),
                laboratorio,
                EstadoEquipo.valueOf(resultado.getString("estado")),
                resultado.getString("observaciones"));
        return new DetallePrestamo(resultado.getInt("id_detalle"), resultado.getInt("id_prestamo"), equipo);
    }
}
