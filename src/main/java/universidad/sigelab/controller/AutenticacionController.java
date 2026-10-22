package universidad.sigelab.controller;

import universidad.sigelab.model.Usuario;
import universidad.sigelab.service.AutenticacionService;

public class AutenticacionController {

    private final AutenticacionService servicioAutenticacion = new AutenticacionService();

    /**
     * Usuario + contraseña + validación facial. Abre la cámara, así que puede tardar unos segundos:
     * la vista debe llamarlo fuera del hilo de la interfaz.
     */
    public Usuario iniciarSesion(String nombreUsuario, String contrasena) {
        return servicioAutenticacion.iniciarSesion(nombreUsuario, contrasena);
    }

    public void cerrarSesion() {
        servicioAutenticacion.cerrarSesion();
    }
}
