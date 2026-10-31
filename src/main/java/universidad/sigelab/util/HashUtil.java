package universidad.sigelab.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class HashUtil {

    /**
     * Nota de seguridad: el script SQL y los usuarios de prueba usan SHA-256 sin sal.
     * Se conserva por compatibilidad con la base del curso; en un sistema en producción
     * conviene migrar a un algoritmo con sal y costo (BCrypt, Argon2 o PBKDF2).
     */
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

    /**
     * Compara una contraseña escrita por el usuario con el hash guardado.
     * Usa MessageDigest.isEqual para no filtrar información por el tiempo de respuesta
     * y tolera hashes guardados en minúsculas o con espacios accidentales.
     */
    public static boolean coincide(String textoPlano, String hashGuardado) {
        if (textoPlano == null || hashGuardado == null || hashGuardado.isBlank()) {
            return false;
        }
        String hashEsperado = hashGuardado.trim().toUpperCase(Locale.ROOT);
        String hashCalculado = sha256(textoPlano);
        return MessageDigest.isEqual(
                hashCalculado.getBytes(StandardCharsets.US_ASCII),
                hashEsperado.getBytes(StandardCharsets.US_ASCII));
    }
}
