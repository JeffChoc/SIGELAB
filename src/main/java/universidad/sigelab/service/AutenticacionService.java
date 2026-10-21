package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.repository.UsuarioRepository;
import universidad.sigelab.util.HashUtil;
import universidad.sigelab.util.SesionUsuario;

import java.util.Optional;

/**
 * Login en dos pasos (sección 6.1 de la guía):
 *   1) validarCredenciales: usuario + contraseña + usuario ACTIVO (RN-01).
 *   2) validación facial (se agrega en la Etapa 4, RN-02).
 *   3) completarInicioSesion: abre la sesión y audita LOGIN_EXITOSO.
 */
public class AutenticacionService {

    private final UsuarioRepository repositorioUsuario = new UsuarioRepository();
    private final AuditoriaService servicioAuditoria = new AuditoriaService();

    public Usuario validarCredenciales(String nombreUsuario, String contrasena) {
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

    /** Se llama cuando TODOS los factores pasaron (hoy solo credenciales; luego, también el rostro). */
    public void completarInicioSesion(Usuario usuario) {
        if (usuario == null || usuario.getEstado() != EstadoRegistro.ACTIVO) {
            throw new BusinessException("No se puede iniciar sesión con este usuario.");
        }
        SesionUsuario.iniciar(usuario);
        servicioAuditoria.registrar(usuario.getIdUsuario(), AccionAuditoria.LOGIN_EXITOSO, "Usuario",
                usuario.getIdUsuario(), "Inicio de sesión de '" + usuario.getNombreUsuario() + "'");
    }

    public void cerrarSesion() {
        SesionUsuario.cerrar();
    }

    private boolean coincideContrasena(Usuario usuario, String contrasenaIngresada) {
        String hashIngresado = HashUtil.sha256(contrasenaIngresada);
        return hashIngresado.equalsIgnoreCase(usuario.getContrasenaHash());
    }
}
