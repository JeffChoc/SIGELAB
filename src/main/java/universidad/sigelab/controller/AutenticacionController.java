package universidad.sigelab.controller;

import universidad.sigelab.model.Usuario;
import universidad.sigelab.service.AutenticacionService;

public class AutenticacionController {

    private final AutenticacionService servicioAutenticacion = new AutenticacionService();

    /** Paso 1 del login. Devuelve el usuario si las credenciales son válidas y está ACTIVO. */
    public Usuario validarCredenciales(String nombreUsuario, String contrasena) {
        return servicioAutenticacion.validarCredenciales(nombreUsuario, contrasena);
    }

    /** Último paso: abre la sesión. Se llamará después de la validación facial. */
    public void completarInicioSesion(Usuario usuario) {
        servicioAutenticacion.completarInicioSesion(usuario);
    }

    /** Atajo TEMPORAL mientras no exista el reconocimiento facial (Etapa 4). */
    public Usuario iniciarSesionSinFacial(String nombreUsuario, String contrasena) {
        Usuario usuario = servicioAutenticacion.validarCredenciales(nombreUsuario, contrasena);
        servicioAutenticacion.completarInicioSesion(usuario);
        return usuario;
    }

    public void cerrarSesion() {
        servicioAutenticacion.cerrarSesion();
    }
}
