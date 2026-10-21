package universidad.sigelab.controller;

import universidad.sigelab.model.Rol;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.service.UsuarioService;

import java.util.List;

public class UsuarioController {

    private final UsuarioService servicioUsuario = new UsuarioService();

    public List<Usuario> listar() {
        return servicioUsuario.listar();
    }

    public List<Rol> rolesParaCombo() {
        return servicioUsuario.listarRoles();
    }

    public Usuario registrar(String nombre, String apellido, String nombreUsuario, String correo,
                             Rol rol, String contrasena, String referenciaFacial) {
        Usuario usuario = construir(0, nombre, apellido, nombreUsuario, correo, rol, referenciaFacial);
        return servicioUsuario.registrar(usuario, contrasena);
    }

    public Usuario modificar(int idUsuario, String nombre, String apellido, String nombreUsuario, String correo,
                             Rol rol, String nuevaContrasena, String referenciaFacial) {
        Usuario usuario = construir(idUsuario, nombre, apellido, nombreUsuario, correo, rol, referenciaFacial);
        return servicioUsuario.modificar(usuario, nuevaContrasena);
    }

    public void desactivar(int idUsuario) {
        servicioUsuario.desactivar(idUsuario);
    }

    public void activar(int idUsuario) {
        servicioUsuario.activar(idUsuario);
    }

    private Usuario construir(int idUsuario, String nombre, String apellido, String nombreUsuario,
                              String correo, Rol rol, String referenciaFacial) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setReferenciaFacial(referenciaFacial);
        return usuario;
    }
}
