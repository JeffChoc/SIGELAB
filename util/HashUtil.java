package universidad.sigelab.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtil {

    private HashUtil() {
    }

    /** SHA-256 en hexadecimal MAYÚSCULAS: el mismo formato que dejó el script SQL. */
    public static String sha256(String texto) {
        try {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            byte[] bytes = resumen.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM", e);
        }
    }
}
