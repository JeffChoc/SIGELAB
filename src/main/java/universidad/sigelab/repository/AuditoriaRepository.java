
package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.Auditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaRepository {

    /** Inserta usando su propia conexión. */
    public void insertar(Auditoria auditoria) {
        try (Connection conexion = DatabaseConnection.getConnection()) {
            insertar(conexion, auditoria);
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al registrar la auditoría", e);
        }
    }

    /** Inserta dentro de una transacción ajena. */
    public void insertar(Connection conexion, Auditoria auditoria) {
        String sql = """
                INSERT INTO Auditoria
                    (id_usuario, accion, entidad, id_registro, descripcion)
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
            throw new DataAccessException(
                    "Error al registrar la auditoría "
                            + auditoria.getAccion(), e);
        }
    }

    /**
     * Consulta auditorías por fechas, acción y nombre de usuario.
     * Los filtros nulos o vacíos se consideran opcionales.
     */
    public List<Auditoria> consultar(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            AccionAuditoria accion,
            String usuario) {

        List<Auditoria> auditorias = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT a.id_auditoria,
                       a.id_usuario,
                       a.fecha_hora,
                       a.accion,
                       a.entidad,
                       a.id_registro,
                       a.descripcion
                FROM Auditoria a
                LEFT JOIN Usuario u ON a.id_usuario = u.id_usuario
                WHERE 1 = 1
                """);

        if (fechaInicio != null) {
            sql.append(" AND a.fecha_hora >= ?");
        }

        if (fechaFin != null) {
            sql.append(" AND a.fecha_hora < ?");
        }

        if (accion != null) {
            sql.append(" AND a.accion = ?");
        }

        if (usuario != null && !usuario.isBlank()) {
            sql.append(" AND u.nombre_usuario LIKE ?");
        }

        sql.append(" ORDER BY a.fecha_hora DESC");

        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia =
                     conexion.prepareStatement(sql.toString())) {

            int indice = 1;

            if (fechaInicio != null) {
                sentencia.setTimestamp(
                        indice++,
                        Timestamp.valueOf(fechaInicio.atStartOfDay()));
            }

            if (fechaFin != null) {
                sentencia.setTimestamp(
                        indice++,
                        Timestamp.valueOf(fechaFin.plusDays(1).atStartOfDay()));
            }

            if (accion != null) {
                sentencia.setString(indice++, accion.name());
            }

            if (usuario != null && !usuario.isBlank()) {
                sentencia.setString(indice, "%" + usuario.trim() + "%");
            }

            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    Auditoria auditoria = new Auditoria();

                    auditoria.setIdAuditoria(
                            resultado.getInt("id_auditoria"));

                    int idUsuario = resultado.getInt("id_usuario");
                    auditoria.setIdUsuario(
                            resultado.wasNull() ? null : idUsuario);

                    Timestamp fechaHora =
                            resultado.getTimestamp("fecha_hora");

                    if (fechaHora != null) {
                        auditoria.setFechaHora(fechaHora.toLocalDateTime());
                    }

                    String nombreAccion = resultado.getString("accion");
                    if (nombreAccion != null) {
                        auditoria.setAccion(
                                AccionAuditoria.valueOf(nombreAccion));
                    }

                    auditoria.setEntidad(resultado.getString("entidad"));

                    int idRegistro = resultado.getInt("id_registro");
                    auditoria.setIdRegistro(
                            resultado.wasNull() ? null : idRegistro);

                    auditoria.setDescripcion(
                            resultado.getString("descripcion"));

                    auditorias.add(auditoria);
                }
            }

            return auditorias;

        } catch (SQLException | IllegalArgumentException e) {
            throw new DataAccessException(
                    "Error al consultar las auditorías", e);
        }
    }
}
