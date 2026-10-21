package universidad.sigelab.repository;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.exception.DataAccessException;
import universidad.sigelab.model.ResultadoReporte;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Consultas de reporte: SELECT, JOIN, WHERE, ORDER BY, BETWEEN, COUNT y GROUP BY (sección 6.11).
 * Devuelven filas ya formateadas (las fechas como texto) para mostrarlas directamente en una tabla.
 */
public class ReporteRepository {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] TITULOS_PRESTAMOS = {
            "N.º", "Solicitante", "Fecha del préstamo", "Devolución esperada",
            "Estado", "Equipos", "Pendientes", "Registrado por"};

    private static final String PENDIENTES = "SUM(CASE WHEN dv.id_devolucion IS NULL THEN 1 ELSE 0 END)";

    private static final String BASE_PRESTAMOS =
            "SELECT p.id_prestamo, p.solicitante_nombre, p.fecha_prestamo, p.fecha_esperada_devolucion, "
                    + "p.estado, COUNT(d.id_detalle) AS total_equipos, " + PENDIENTES + " AS pendientes, "
                    + "u.nombre_usuario AS registrado_por "
                    + "FROM Prestamo p "
                    + "JOIN Usuario u ON u.id_usuario = p.id_usuario_registra "
                    + "JOIN DetallePrestamo d ON d.id_prestamo = p.id_prestamo "
                    + "LEFT JOIN Devolucion dv ON dv.id_detalle = d.id_detalle ";

    private static final String AGRUPAR_PRESTAMOS =
            "GROUP BY p.id_prestamo, p.solicitante_nombre, p.fecha_prestamo, p.fecha_esperada_devolucion, "
                    + "p.estado, u.nombre_usuario ";

    private static final String ORDENAR_PRESTAMOS = "ORDER BY p.fecha_prestamo DESC";

    @FunctionalInterface
    private interface AsignadorParametros {
        void asignar(PreparedStatement sentencia) throws SQLException;
    }

    // ---------- Equipos ----------

    public ResultadoReporte equiposPorEstado() {
        return consultar("SELECT estado, COUNT(*) AS cantidad FROM Equipo GROUP BY estado ORDER BY estado",
                sentencia -> { }, "Estado", "Cantidad");
    }

    public ResultadoReporte equiposPorLaboratorioYEstado() {
        String sql = "SELECT l.codigo, l.nombre, e.estado, COUNT(*) AS cantidad "
                + "FROM Equipo e JOIN Laboratorio l ON l.id_laboratorio = e.id_laboratorio "
                + "GROUP BY l.codigo, l.nombre, e.estado ORDER BY l.codigo, e.estado";
        return consultar(sql, sentencia -> { }, "Laboratorio", "Nombre", "Estado", "Cantidad");
    }

    // ---------- Préstamos ----------

    public ResultadoReporte prestamosTodos() {
        return consultar(BASE_PRESTAMOS + AGRUPAR_PRESTAMOS + ORDENAR_PRESTAMOS,
                sentencia -> { }, TITULOS_PRESTAMOS);
    }

    public ResultadoReporte prestamosActivos() {
        return consultar(BASE_PRESTAMOS + "WHERE p.estado = 'ACTIVO' " + AGRUPAR_PRESTAMOS + ORDENAR_PRESTAMOS,
                sentencia -> { }, TITULOS_PRESTAMOS);
    }

    public ResultadoReporte prestamosFinalizados() {
        return consultar(BASE_PRESTAMOS + "WHERE p.estado = 'FINALIZADO' " + AGRUPAR_PRESTAMOS + ORDENAR_PRESTAMOS,
                sentencia -> { }, TITULOS_PRESTAMOS);
    }

    /** RN-17: vencido = la fecha esperada ya pasó y todavía hay equipos pendientes. */
    public ResultadoReporte prestamosVencidos() {
        String having = "HAVING " + PENDIENTES + " > 0 AND p.fecha_esperada_devolucion < SYSDATETIME() ";
        return consultar(BASE_PRESTAMOS + AGRUPAR_PRESTAMOS + having + ORDENAR_PRESTAMOS,
                sentencia -> { }, TITULOS_PRESTAMOS);
    }

    public ResultadoReporte prestamosEntreFechas(LocalDateTime desde, LocalDateTime hasta) {
        return consultar(BASE_PRESTAMOS + "WHERE p.fecha_prestamo BETWEEN ? AND ? "
                        + AGRUPAR_PRESTAMOS + ORDENAR_PRESTAMOS,
                sentencia -> {
                    sentencia.setTimestamp(1, Timestamp.valueOf(desde));
                    sentencia.setTimestamp(2, Timestamp.valueOf(hasta));
                }, TITULOS_PRESTAMOS);
    }

    // ---------- Historiales de un equipo ----------

    public ResultadoReporte historialPrestamosDeEquipo(String codigoInventario) {
        String sql = "SELECT p.id_prestamo, p.solicitante_nombre, p.fecha_prestamo, "
                + "p.fecha_esperada_devolucion, dv.fecha_devolucion, "
                + "COALESCE(dv.condicion, 'PENDIENTE') AS condicion, dv.observaciones "
                + "FROM Equipo e "
                + "JOIN DetallePrestamo d ON d.id_equipo = e.id_equipo "
                + "JOIN Prestamo p ON p.id_prestamo = d.id_prestamo "
                + "LEFT JOIN Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE e.codigo_inventario = ? ORDER BY p.fecha_prestamo DESC";
        return consultar(sql, sentencia -> sentencia.setString(1, codigoInventario),
                "N.º préstamo", "Solicitante", "Fecha del préstamo", "Devolución esperada",
                "Fecha de devolución", "Condición", "Observaciones");
    }

    public ResultadoReporte historialMantenimientoDeEquipo(String codigoInventario) {
        String sql = "SELECT m.id_mantenimiento, m.tipo, m.fecha_ingreso, m.fecha_salida, "
                + "COALESCE(m.resultado, '-') AS resultado, m.estado, m.responsable, m.descripcion "
                + "FROM Mantenimiento m JOIN Equipo e ON e.id_equipo = m.id_equipo "
                + "WHERE e.codigo_inventario = ? ORDER BY m.fecha_ingreso DESC";
        return consultar(sql, sentencia -> sentencia.setString(1, codigoInventario),
                "N.º", "Tipo", "Fecha de ingreso", "Fecha de salida", "Resultado",
                "Estado", "Responsable", "Descripción");
    }

    // ---------- Utilidades privadas ----------

    private ResultadoReporte consultar(String sql, AsignadorParametros asignador, String... titulos) {
        List<Object[]> filas = new ArrayList<>();
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            asignador.asignar(sentencia);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    Object[] fila = new Object[titulos.length];
                    for (int i = 0; i < titulos.length; i++) {
                        fila[i] = formatear(resultado.getObject(i + 1));
                    }
                    filas.add(fila);
                }
            }
            return new ResultadoReporte(Arrays.asList(titulos), filas);
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar el reporte", e);
        }
    }

    private Object formatear(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Timestamp) {
            return ((Timestamp) valor).toLocalDateTime().format(FORMATO_FECHA);
        }
        if (valor instanceof LocalDateTime) {
            return ((LocalDateTime) valor).format(FORMATO_FECHA);
        }
        return valor;
    }
}
