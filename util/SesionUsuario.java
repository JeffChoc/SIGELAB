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

    /** Valida el permiso en la lógica de negocio, sin depender de botones ocultos. */
    public static void exigirRol(RolUsuario... rolesPermitidos) {
        exigirSesion();
        boolean permitido = Arrays.asList(rolesPermitidos).contains(usuarioActual.getRol().getNombre());
        if (!permitido) {
            throw new BusinessException("No tiene permisos para realizar esta operación.");
        }
    }
}
