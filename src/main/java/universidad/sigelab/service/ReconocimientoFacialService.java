package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.ResultadoFacial;
import universidad.sigelab.util.SesionUsuario;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente Java del servicio facial (Python + OpenCV, ver carpeta facial/).
 * Java solo consume el servicio, interpreta la respuesta y decide si bloquea el acceso (RN-02).
 */
public class ReconocimientoFacialService {

    private static final String URL_POR_DEFECTO = "http://127.0.0.1:5001";
    private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(3);
    private static final Duration TIEMPO_VERIFICACION = Duration.ofSeconds(40);
    private static final Duration TIEMPO_REGISTRO = Duration.ofSeconds(60);

    private static final Pattern PATRON_EXITO = Pattern.compile("\"exito\"\\s*:\\s*(true|false)");
    private static final Pattern PATRON_MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final HttpClient clienteHttp = HttpClient.newBuilder().connectTimeout(TIEMPO_CONEXION).build();
    private final String urlBase = leerUrlConfigurada();
    private final AuditoriaService servicioAuditoria = new AuditoriaService();

    /**
     * Verifica que el rostro frente a la cámara corresponda al usuario.
     * No exige sesión: se usa durante el login, cuando todavía no existe.
     * Lanza BusinessException si el servicio no responde.
     */
    public ResultadoFacial verificar(int idUsuario) {
        return llamar("/verificar", idUsuario, TIEMPO_VERIFICACION);
    }

    /** Registra el rostro de un usuario con la cámara del equipo. Solo ADMIN. */
    public void registrarRostro(int idUsuario) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        ResultadoFacial resultado = llamar("/registrar", idUsuario, TIEMPO_REGISTRO);
        if (!resultado.isExito()) {
            throw new BusinessException(resultado.getMensaje());
        }
        servicioAuditoria.registrarComoUsuarioActual(AccionAuditoria.USUARIO_MODIFICADO, "Usuario", idUsuario,
                "Se registró el rostro del usuario " + idUsuario);
    }

    // ---------- Privados ----------

    private ResultadoFacial llamar(String ruta, int idUsuario, Duration tiempoMaximo) {
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(urlBase + ruta + "?usuario=" + idUsuario))
                .timeout(tiempoMaximo)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<String> respuesta = clienteHttp.send(solicitud,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return interpretar(respuesta.body());
        } catch (HttpTimeoutException e) {
            throw new BusinessException("El servicio de reconocimiento facial tardó demasiado en responder.");
        } catch (IOException e) {
            throw new BusinessException("No se pudo conectar con el servicio de reconocimiento facial. "
                    + "Verifique que servidor_facial.py esté en ejecución.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("La verificación facial fue interrumpida.");
        }
    }

    private ResultadoFacial interpretar(String cuerpo) {
        Matcher exito = PATRON_EXITO.matcher(cuerpo == null ? "" : cuerpo);
        if (!exito.find()) {
            throw new BusinessException("El servicio de reconocimiento facial devolvió una respuesta no válida.");
        }
        Matcher mensaje = PATRON_MENSAJE.matcher(cuerpo);
        String texto = mensaje.find() ? mensaje.group(1).replace("\\\"", "\"") : "";
        return new ResultadoFacial(Boolean.parseBoolean(exito.group(1)), texto);
    }

    /** La dirección del servicio puede cambiarse con facial.url en config.properties (opcional). */
    private static String leerUrlConfigurada() {
        Path ruta = Paths.get("config.properties");
        if (Files.exists(ruta)) {
            Properties propiedades = new Properties();
            try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
                propiedades.load(lector);
                String url = propiedades.getProperty("facial.url");
                if (url != null && !url.isBlank()) {
                    return url.trim().replaceAll("/+$", "");
                }
            } catch (IOException ignorada) {
                // si no se puede leer, se usa la dirección por defecto
            }
        }
        return URL_POR_DEFECTO;
    }
}
