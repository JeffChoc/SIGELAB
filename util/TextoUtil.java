package universidad.sigelab.util;

public final class TextoUtil {

    private TextoUtil() {
    }

    /** Quita espacios al inicio y al final; un texto vacío se convierte en null. */
    public static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
