package universidad.sigelab.exception;

/** Envuelve los errores técnicos de JDBC. Su mensaje NO se muestra al usuario. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
