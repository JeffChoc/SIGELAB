package universidad.sigelab.exception;

/** Se lanza cuando se incumple una regla de negocio. El mensaje es apto para mostrarse al usuario. */
public class BusinessException extends RuntimeException {

    public BusinessException(String mensaje) {
        super(mensaje);
    }
}
