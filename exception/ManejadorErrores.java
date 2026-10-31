package universidad.sigelab.exception;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Convierte cualquier excepción en un mensaje comprensible para el usuario. */
public final class ManejadorErrores {

    private static final Logger LOGGER = Logger.getLogger(ManejadorErrores.class.getName());

    private ManejadorErrores() {
    }

    public static String mensajeParaUsuario(Throwable error) {
        if (error instanceof BusinessException) {
            return error.getMessage();
        }
        if (error instanceof DataAccessException) {
            LOGGER.log(Level.SEVERE, error.getMessage(), error.getCause());
            return "No se pudo completar la operación por un problema con la base de datos. Intente de nuevo.";
        }
        LOGGER.log(Level.SEVERE, "Error inesperado", error);
        return "Ocurrió un error inesperado. Intente de nuevo.";
    }
}
