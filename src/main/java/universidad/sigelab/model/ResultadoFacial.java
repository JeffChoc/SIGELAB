package universidad.sigelab.model;

/** Respuesta del servicio facial: si la operación tuvo éxito y un mensaje apto para el usuario. */
public class ResultadoFacial {

    private final boolean exito;
    private final String mensaje;

    public ResultadoFacial(boolean exito, String mensaje) {
        this.exito = exito;
        this.mensaje = mensaje;
    }

    public boolean isExito() { return exito; }
    public String getMensaje() { return mensaje; }
}
