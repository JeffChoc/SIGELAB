package universidad.sigelab.util;

import universidad.sigelab.config.DatabaseConnection;
import universidad.sigelab.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ejecuta varias operaciones como una sola unidad: si algo falla, todo se revierte
 * y no queda información inconsistente. La usarán también préstamos y devoluciones.
 */
public final class Transaccion {

    @FunctionalInterface
    public interface Operacion {
        void ejecutar(Connection conexion);
    }

    private Transaccion() {
    }

    public static void ejecutar(Operacion operacion) {
        try (Connection conexion = DatabaseConnection.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                operacion.ejecutar(conexion);
                conexion.commit();
            } catch (RuntimeException e) {
                revertir(conexion);
                throw e; // BusinessException o DataAccessException siguen su camino hacia la vista
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al manejar la transacción", e);
        }
    }

    private static void revertir(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException ignorada) {
            // si el rollback falla, la conexión se cierra igualmente y SQL Server descarta lo pendiente
        }
    }
}
