package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.enums.EstadoEquipo;
import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.Laboratorio;
import universidad.sigelab.model.TipoEquipo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Solo acceso a datos: aquí no hay reglas de negocio. */
public class EquipoRepository {

    private static final String SELECT_BASE = """
            SELECT e.id_equipo, e.codigo_inventario, e.nombre, e.marca, e.modelo,
                   e.numero_serie, e.estado, e.observaciones,
                   t.id_tipo_equipo, t.nombre AS tipo_nombre, t.descripcion AS tipo_descripcion,
                   l.id_laboratorio, l.codigo AS lab_codigo, l.nombre AS lab_nombre,
                   l.ubicacion AS lab_ubicacion, l.capacidad AS lab_capacidad, l.estado AS lab_estado
            FROM Equipo e
            JOIN TipoEquipo t ON t.id_tipo_equipo = e.id_tipo_equipo
            JOIN Laboratorio l ON l.id_laboratorio = e.id_laboratorio
            """;

    @FunctionalInterface
    private interface AsignadorParametros {
        void asignar(PreparedStatement sentencia) throws SQLException;
    }

    // ---------- Consultas ----------

    public List<Equipo> listarTodos() {
        return consultarLista(SELECT_BASE + " ORDER BY e.codigo_inventario", sentencia -> { });
    }

    public List<Equipo> listarPorEstado(EstadoEquipo estado) {
        return consultarLista(SELECT_BASE + " WHERE e.estado = ? ORDER BY e.codigo_inventario",
                sentencia -> sentencia.setString(1, estado.name()));
    }

    public Optional<Equipo> buscarPorId(int idEquipo) {
        return consultarLista(SELECT_BASE + " WHERE e.id_equipo = ?",
                sentencia -> sentencia.setInt(1, idEquipo)).stream().findFirst();
    }

    public Optional<Equipo> buscarPorCodigo(String codigoInventario) {
        return consultarLista(SELECT_BASE + " WHERE e.codigo_inventario = ?",
                sentencia -> sentencia.setString(1, codigoInventario)).stream().findFirst();
    }

    /** idExcluido = 0 cuando se registra uno nuevo; el id propio cuando se modifica. */
    public boolean existeCodigo(String codigoInventario, int idExcluido) {
        return contar("SELECT COUNT(*) FROM Equipo WHERE codigo_inventario = ? AND id_equipo <> ?",
                sentencia -> {
                    sentencia.setString(1, codigoInventario);
                    sentencia.setInt(2, idExcluido);
                }) > 0;
    }

    public boolean existeNumeroSerie(String numeroSerie, int idExcluido) {
        return contar("SELECT COUNT(*) FROM Equipo WHERE numero_serie = ? AND id_equipo <> ?",
                sentencia -> {
                    sentencia.setString(1, numeroSerie);
                    sentencia.setInt(2, idExcluido);
                }) > 0;
    }

    // ---------- Escritura ----------

    /** Devuelve el id generado por SQL Server. */
    public int insertar(Equipo equipo) {
        String sql = """
                INSERT INTO Equipo (codigo_inventario, nombre, id_tipo_equipo, marca, modelo,
                                    numero_serie, id_laboratorio, estado, observaciones)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, equipo.getCodigoInventario());
            sentencia.setString(2, equipo.getNombre());
            sentencia.setInt(3, equipo.getTipoEquipo().getIdTipoEquipo());
            sentencia.setString(4, equipo.getMarca());
            sentencia.setString(5, equipo.getModelo());
            sentencia.setString(6, equipo.getNumeroSerie());
            sentencia.setInt(7, equipo.getLaboratorio().getIdLaboratorio());
            sentencia.setString(8, equipo.getEstado().name());
            sentencia.setString(9, equipo.getObservaciones());
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
                throw new DataAccessException("No se obtuvo el id generado del equipo", null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al insertar el equipo " + equipo.getCodigoInventario(), e);
        }
    }

    /** Actualiza los datos del equipo. El estado NO se toca aquí: cambia solo por operaciones de negocio. */
    public void actualizar(Equipo equipo) {
        String sql = """
                UPDATE Equipo
                SET codigo_inventario = ?, nombre = ?, id_tipo_equipo = ?, marca = ?, modelo = ?,
                    numero_serie = ?, id_laboratorio = ?, observaciones = ?
                WHERE id_equipo = ?
                """;
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, equipo.getCodigoInventario());
            sentencia.setString(2, equipo.getNombre());
            sentencia.setInt(3, equipo.getTipoEquipo().getIdTipoEquipo());
            sentencia.setString(4, equipo.getMarca());
            sentencia.setString(5, equipo.getModelo());
            sentencia.setString(6, equipo.getNumeroSerie());
            sentencia.setInt(7, equipo.getLaboratorio().getIdLaboratorio());
            sentencia.setString(8, equipo.getObservaciones());
            sentencia.setInt(9, equipo.getIdEquipo());
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al actualizar el equipo " + equipo.getIdEquipo(), e);
        }
    }

    /**
     * Cambia el estado dentro de una transacción ajena (recibe la Connection).
     * Solo actualiza si el equipo sigue en el estado esperado: evita que dos operaciones
     * simultáneas pisen el mismo equipo. Devuelve false si el estado ya no era el esperado.
     */
    public boolean actualizarEstado(Connection conexion, int idEquipo,
                                    EstadoEquipo estadoEsperado, EstadoEquipo estadoNuevo) {
        String sql = "UPDATE Equipo SET estado = ? WHERE id_equipo = ? AND estado = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, estadoNuevo.name());
            sentencia.setInt(2, idEquipo);
            sentencia.setString(3, estadoEsperado.name());
            return sentencia.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Error al cambiar el estado del equipo " + idEquipo, e);
        }
    }

    // ---------- Utilidades privadas ----------

    private List<Equipo> consultarLista(String sql, AsignadorParametros asignador) {
        List<Equipo> equipos = new ArrayList<>();
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            asignador.asignar(sentencia);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    equipos.add(mapear(resultado));
                }
            }
            return equipos;
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar equipos", e);
        }
    }

    private int contar(String sql, AsignadorParametros asignador) {
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            asignador.asignar(sentencia);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al verificar duplicados de equipo", e);
        }
    }

    private Equipo mapear(ResultSet resultado) throws SQLException {
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
        return new Equipo(
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
    }
}
