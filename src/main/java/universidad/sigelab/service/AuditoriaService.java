package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.model.Auditoria;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.repository.AuditoriaRepository;
import universidad.sigelab.util.SesionUsuario;

/** RN-18: las operaciones relevantes dejan rastro. */
public class AuditoriaService {

    private static final int LARGO_MAXIMO_DESCRIPCION = 300;

    private final AuditoriaRepository repositorioAuditoria = new AuditoriaRepository();

    public void registrar(Integer idUsuario, AccionAuditoria accion, String entidad,
                          Integer idRegistro, String descripcion) {
        repositorioAuditoria.insertar(new Auditoria(idUsuario, accion, entidad, idRegistro, recortar(descripcion)));
    }

    /** Registra la acción a nombre del usuario que tiene la sesión abierta. */
    public void registrarComoUsuarioActual(AccionAuditoria accion, String entidad,
                                           Integer idRegistro, String descripcion) {
        Usuario usuario = SesionUsuario.getUsuarioActual();
        Integer idUsuario = usuario == null ? null : usuario.getIdUsuario();
        registrar(idUsuario, accion, entidad, idRegistro, descripcion);
    }

    private String recortar(String texto) {
        if (texto == null || texto.length() <= LARGO_MAXIMO_DESCRIPCION) {
            return texto;
        }
        return texto.substring(0, LARGO_MAXIMO_DESCRIPCION);
    }
}
