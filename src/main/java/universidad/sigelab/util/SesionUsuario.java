package universidad.sigelab.util;

import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Usuario;

import java.util.Arrays;

/** Usuario que inició sesión. Los Service lo usan para validar permisos (RN-20) y auditar. */
public final class SesionUsuario {

    private static Usuario usuarioActual;

    private SesionUsuario() {
    }

    public static void iniciar(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("No se puede iniciar una sesión sin usuario.");
        }
        if (usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            throw new IllegalArgumentException("El usuario de la sesión no tiene un rol válido.");
        }
        usuarioActual = usuario;
    }

    public static void cerrar() {
        usuarioActual = null;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static boolean haySesion() {
        return usuarioActual != null;
    }

    public static void exigirSesion() {
        if (usuarioActual == null) {
            throw new BusinessException("Debe iniciar sesión para continuar.");
        }
    }

    /** Rol de la sesión o null si no hay nadie autenticado. */
    public static RolUsuario getRol() {
        return usuarioActual == null || usuarioActual.getRol() == null
                ? null
                : usuarioActual.getRol().getNombre();
    }

    /** Valida el permiso en la lógica de negocio, sin depender de botones ocultos. */
    public static void exigirRol(RolUsuario... rolesPermitidos) {
        exigirSesion();
        RolUsuario rolActual = getRol();
        if (rolActual == null || rolesPermitidos == null
                || !Arrays.asList(rolesPermitidos).contains(rolActual)) {
            throw new BusinessException("No tiene permisos para realizar esta operación.");
        }
    }

    /** Actualiza los datos visibles del usuario de la sesión (por ejemplo, tras editarlo). */
    public static void actualizarDatos(Usuario usuario) {
        if (usuario == null || usuarioActual == null) {
            return;
        }
        if (usuario.getIdUsuario() == usuarioActual.getIdUsuario()) {
            usuarioActual = usuario;
        }
    }
}
