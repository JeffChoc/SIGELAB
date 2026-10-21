package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Rol;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.repository.RolRepository;
import universidad.sigelab.repository.UsuarioRepository;
import universidad.sigelab.util.HashUtil;
import universidad.sigelab.util.SesionUsuario;
import universidad.sigelab.util.TextoUtil;

import java.util.List;
import java.util.regex.Pattern;

/** Gestión de usuarios: solo ADMIN. Desactivación lógica para conservar el historial. */
public class UsuarioService {

    private static final Pattern PATRON_USUARIO = Pattern.compile("[A-Za-z0-9._-]{3,40}");
    private static final Pattern PATRON_CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final int LARGO_MINIMO_CONTRASENA = 8;

    private final UsuarioRepository repositorioUsuario = new UsuarioRepository();
    private final RolRepository repositorioRol = new RolRepository();
    private final AuditoriaService servicioAuditoria = new AuditoriaService();

    public List<Usuario> listar() {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        List<Usuario> usuarios = repositorioUsuario.listarTodos();
        usuarios.forEach(usuario -> usuario.setContrasenaHash(null)); // el hash no sale del Service
        return usuarios;
    }

    public List<Rol> listarRoles() {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        return repositorioRol.listarTodos();
    }

    public Usuario registrar(Usuario usuario, String contrasena) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        normalizar(usuario);
        validarDatos(usuario);
        validarContrasena(contrasena);
        usuario.setRol(validarRol(usuario));
        validarDuplicados(usuario, 0);

        usuario.setEstado(EstadoRegistro.ACTIVO);
        usuario.setContrasenaHash(HashUtil.sha256(contrasena));
        int idGenerado = repositorioUsuario.insertar(usuario);
        usuario.setIdUsuario(idGenerado);
        usuario.setContrasenaHash(null);

        servicioAuditoria.registrarComoUsuarioActual(AccionAuditoria.USUARIO_REGISTRADO, "Usuario", idGenerado,
                "Se registró el usuario '" + usuario.getNombreUsuario() + "'");
        return usuario;
    }

    /** nuevaContrasena vacía o null = no cambiar la contraseña. */
    public Usuario modificar(Usuario usuario, String nuevaContrasena) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        Usuario existente = repositorioUsuario.buscarPorId(usuario.getIdUsuario())
                .orElseThrow(() -> new BusinessException("El usuario que intenta modificar ya no existe."));

        normalizar(usuario);
        validarDatos(usuario);
        usuario.setRol(validarRol(usuario));
        validarDuplicados(usuario, usuario.getIdUsuario());

        boolean esUsuarioActual = SesionUsuario.getUsuarioActual().getIdUsuario() == usuario.getIdUsuario();
        if (esUsuarioActual && existente.getRol().getIdRol() != usuario.getRol().getIdRol()) {
            throw new BusinessException("No puede cambiar su propio rol.");
        }

        boolean cambiaContrasena = nuevaContrasena != null && !nuevaContrasena.isEmpty();
        if (cambiaContrasena) {
            validarContrasena(nuevaContrasena);
        }

        usuario.setEstado(existente.getEstado());
        repositorioUsuario.actualizar(usuario);
        if (cambiaContrasena) {
            repositorioUsuario.actualizarContrasena(usuario.getIdUsuario(), HashUtil.sha256(nuevaContrasena));
        }

        servicioAuditoria.registrarComoUsuarioActual(AccionAuditoria.USUARIO_MODIFICADO, "Usuario",
                usuario.getIdUsuario(), "Se modificó el usuario '" + usuario.getNombreUsuario() + "'"
                        + (cambiaContrasena ? " (con cambio de contraseña)" : ""));
        return usuario;
    }

    public void desactivar(int idUsuario) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        if (SesionUsuario.getUsuarioActual().getIdUsuario() == idUsuario) {
            throw new BusinessException("No puede desactivar su propio usuario.");
        }
        Usuario existente = repositorioUsuario.buscarPorId(idUsuario)
                .orElseThrow(() -> new BusinessException("El usuario seleccionado ya no existe."));
        if (existente.getEstado() == EstadoRegistro.INACTIVO) {
            throw new BusinessException("El usuario " + existente.getNombreUsuario() + " ya está inactivo.");
        }
        repositorioUsuario.actualizarEstado(idUsuario, EstadoRegistro.INACTIVO);
        servicioAuditoria.registrarComoUsuarioActual(AccionAuditoria.USUARIO_DESACTIVADO, "Usuario", idUsuario,
                "Se desactivó el usuario '" + existente.getNombreUsuario() + "'");
    }

    public void activar(int idUsuario) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        Usuario existente = repositorioUsuario.buscarPorId(idUsuario)
                .orElseThrow(() -> new BusinessException("El usuario seleccionado ya no existe."));
        if (existente.getEstado() == EstadoRegistro.ACTIVO) {
            throw new BusinessException("El usuario " + existente.getNombreUsuario() + " ya está activo.");
        }
        repositorioUsuario.actualizarEstado(idUsuario, EstadoRegistro.ACTIVO);
        servicioAuditoria.registrarComoUsuarioActual(AccionAuditoria.USUARIO_ACTIVADO, "Usuario", idUsuario,
                "Se activó el usuario '" + existente.getNombreUsuario() + "'");
    }

    // ---------- Validaciones privadas ----------

    private void normalizar(Usuario usuario) {
        usuario.setNombre(TextoUtil.limpiar(usuario.getNombre()));
        usuario.setApellido(TextoUtil.limpiar(usuario.getApellido()));
        usuario.setNombreUsuario(TextoUtil.limpiar(usuario.getNombreUsuario()));
        usuario.setCorreo(TextoUtil.limpiar(usuario.getCorreo()));
        usuario.setReferenciaFacial(TextoUtil.limpiar(usuario.getReferenciaFacial()));
    }

    private void validarDatos(Usuario usuario) {
        if (usuario.getNombre() == null) {
            throw new BusinessException("El nombre es obligatorio.");
        }
        if (usuario.getApellido() == null) {
            throw new BusinessException("El apellido es obligatorio.");
        }
        if (usuario.getNombreUsuario() == null) {
            throw new BusinessException("El nombre de usuario es obligatorio.");
        }
        if (usuario.getCorreo() == null) {
            throw new BusinessException("El correo es obligatorio.");
        }
        if (usuario.getRol() == null) {
            throw new BusinessException("Seleccione un rol.");
        }
        if (usuario.getNombre().length() > 60 || usuario.getApellido().length() > 60) {
            throw new BusinessException("El nombre y el apellido no pueden superar 60 caracteres.");
        }
        if (!PATRON_USUARIO.matcher(usuario.getNombreUsuario()).matches()) {
            throw new BusinessException("El nombre de usuario debe tener entre 3 y 40 caracteres "
                    + "y solo puede contener letras, números, punto, guion y guion bajo.");
        }
        if (usuario.getCorreo().length() > 120 || !PATRON_CORREO.matcher(usuario.getCorreo()).matches()) {
            throw new BusinessException("Ingrese un correo válido.");
        }
        if (usuario.getReferenciaFacial() != null && usuario.getReferenciaFacial().length() > 200) {
            throw new BusinessException("La referencia facial no puede superar 200 caracteres.");
        }
    }

    private void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            throw new BusinessException("La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
        }
    }

    /** No se confía en el Rol que llega de la vista: se vuelve a consultar. */
    private Rol validarRol(Usuario usuario) {
        return repositorioRol.buscarPorId(usuario.getRol().getIdRol())
                .orElseThrow(() -> new BusinessException("El rol seleccionado no existe."));
    }

    private void validarDuplicados(Usuario usuario, int idExcluido) {
        if (repositorioUsuario.existeNombreUsuario(usuario.getNombreUsuario(), idExcluido)) {
            throw new BusinessException("Ya existe un usuario con el nombre " + usuario.getNombreUsuario() + ".");
        }
        if (repositorioUsuario.existeCorreo(usuario.getCorreo(), idExcluido)) {
            throw new BusinessException("Ya existe un usuario con el correo " + usuario.getCorreo() + ".");
        }
    }
}
