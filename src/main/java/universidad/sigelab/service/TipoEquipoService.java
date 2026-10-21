package universidad.sigelab.service;

import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.TipoEquipo;
import universidad.sigelab.repository.TipoEquipoRepository;
import universidad.sigelab.util.SesionUsuario;
import universidad.sigelab.util.TextoUtil;

import java.util.List;

public class TipoEquipoService {

    private final TipoEquipoRepository repositorioTipoEquipo = new TipoEquipoRepository();

    public List<TipoEquipo> listar() {
        SesionUsuario.exigirSesion();
        return repositorioTipoEquipo.listarTodos();
    }

    public TipoEquipo registrar(TipoEquipo tipo) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        normalizar(tipo);
        validar(tipo, 0);
        tipo.setIdTipoEquipo(repositorioTipoEquipo.insertar(tipo));
        return tipo;
    }

    public TipoEquipo modificar(TipoEquipo tipo) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        repositorioTipoEquipo.buscarPorId(tipo.getIdTipoEquipo())
                .orElseThrow(() -> new BusinessException("El tipo de equipo que intenta modificar ya no existe."));
        normalizar(tipo);
        validar(tipo, tipo.getIdTipoEquipo());
        repositorioTipoEquipo.actualizar(tipo);
        return tipo;
    }

    private void normalizar(TipoEquipo tipo) {
        tipo.setNombre(TextoUtil.limpiar(tipo.getNombre()));
        tipo.setDescripcion(TextoUtil.limpiar(tipo.getDescripcion()));
    }

    private void validar(TipoEquipo tipo, int idExcluido) {
        if (tipo.getNombre() == null) {
            throw new BusinessException("El nombre del tipo de equipo es obligatorio.");
        }
        if (tipo.getNombre().length() > 60) {
            throw new BusinessException("El nombre no puede superar 60 caracteres.");
        }
        if (tipo.getDescripcion() != null && tipo.getDescripcion().length() > 150) {
            throw new BusinessException("La descripción no puede superar 150 caracteres.");
        }
        if (repositorioTipoEquipo.existeNombre(tipo.getNombre(), idExcluido)) {
            throw new BusinessException("Ya existe un tipo de equipo llamado " + tipo.getNombre() + ".");
        }
    }
}
