package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.ResultadoFacial;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.repository.UsuarioRepository;
import universidad.sigelab.util.HashUtil;
import universidad.sigelab.util.SesionUsuario;

import java.util.Optional;

/**
 * Login en tres pasos obligatorios (sección 6.1 de la guía):
 *   1) usuario + contraseña + usuario ACTIVO (RN-01)
 *   2) validación facial (RN-02): credenciales válidas NO bastan
 *   3) apertura de sesión y auditoría de LOGIN_EXITOSO
 * Todo ocurre dentro de iniciarSesion: la pantalla no puede saltarse el paso del rostro (RN-20).
 */
public class AutenticacionService {

    private final UsuarioRepository repositorioUsuario = new UsuarioRepository();
    private final AuditoriaService servicioAuditoria = new AuditoriaService();
    private final ReconocimientoFacialService servicioFacial = new ReconocimientoFacialService();

    public Usuario iniciarSesion(String nombreUsuario, String contrasena) {
        Usuario usuario = validarCredenciales(nombreUsuario, contrasena);
        verificarRostro(usuario);
        completarInicioSesion(usuario);
        return usuario;
    }

    public void cerrarSesion() {
        SesionUsuario.cerrar();
    }

    // ---------- Paso 1: credenciales ----------

    private Usuario validarCredenciales(String nombreUsuario, String contrasena) {
        if (nombreUsuario == null || nombreUsuario.isBlank() || contrasena == null || contrasena.isEmpty()) {
            throw new BusinessException("Ingrese su usuario y su contraseña.");
        }
        String usuarioLimpio = nombreUsuario.trim();
        Optional<Usuario> encontrado = repositorioUsuario.buscarPorNombreUsuario(usuarioLimpio);

        if (encontrado.isEmpty() || !coincideContrasena(encontrado.get(), contrasena)) {
            Integer idUsuario = encontrado.map(Usuario::getIdUsuario).orElse(null);
            servicioAuditoria.registrar(idUsuario, AccionAuditoria.LOGIN_FALLIDO, "Usuario", idUsuario,
                    "Credenciales incorrectas para el usuario '" + usuarioLimpio + "'");
            // Mismo mensaje exista o no el usuario, para no revelar cuáles usuarios existen.
            throw new BusinessException("Usuario o contraseña incorrectos.");
        }

        Usuario usuario = encontrado.get();

        // RN-01: no se puede iniciar sesión con un usuario INACTIVO.
        if (usuario.getEstado() != EstadoRegistro.ACTIVO) {
            servicioAuditoria.registrar(usuario.getIdUsuario(), AccionAuditoria.LOGIN_FALLIDO, "Usuario",
                    usuario.getIdUsuario(), "Intento de ingreso con usuario inactivo '" + usuarioLimpio + "'");
            throw new BusinessException("El usuario está inactivo. Comuníquese con el administrador.");
        }

        usuario.setContrasenaHash(null); // el hash no debe viajar a la vista ni a la sesión
        return usuario;
    }

    // ---------- Paso 2: rostro ----------

    /** RN-02: si el rostro no se reconoce, o el servicio facial no responde, el acceso se deniega. */
    private void verificarRostro(Usuario usuario) {
        ResultadoFacial resultado;
        try {
            resultado = servicioFacial.verificar(usuario.getIdUsuario());
        } catch (BusinessException e) {
            registrarLoginFallido(usuario, "Verificación facial no disponible: " + e.getMessage());
            throw e;
        }
        if (!resultado.isExito()) {
            registrarLoginFallido(usuario, "Rostro no reconocido: " + resultado.getMensaje());
            throw new BusinessException("Acceso denegado. " + resultado.getMensaje());
        }
    }

    private void registrarLoginFallido(Usuario usuario, String descripcion) {
        servicioAuditoria.registrar(usuario.getIdUsuario(), AccionAuditoria.LOGIN_FALLIDO, "Usuario",
                usuario.getIdUsuario(), descripcion + " (usuario '" + usuario.getNombreUsuario() + "')");
    }

    // ---------- Paso 3: sesión ----------

    private void completarInicioSesion(Usuario usuario) {
        SesionUsuario.iniciar(usuario);
        servicioAuditoria.registrar(usuario.getIdUsuario(), AccionAuditoria.LOGIN_EXITOSO, "Usuario",
                usuario.getIdUsuario(), "Inicio de sesión de '" + usuario.getNombreUsuario() + "'");
    }

    private boolean coincideContrasena(Usuario usuario, String contrasenaIngresada) {
        String hashIngresado = HashUtil.sha256(contrasenaIngresada);
        return hashIngresado.equalsIgnoreCase(usuario.getContrasenaHash());
    }
}
