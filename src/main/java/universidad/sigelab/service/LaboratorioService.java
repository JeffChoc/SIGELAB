package universidad.sigelab.service;

import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Laboratorio;
import universidad.sigelab.repository.LaboratorioRepository;
import universidad.sigelab.util.SesionUsuario;
import universidad.sigelab.util.TextoUtil;

import java.util.List;
import java.util.Locale;

public class LaboratorioService {

    private final LaboratorioRepository repositorioLaboratorio = new LaboratorioRepository();

    public List<Laboratorio> listar() {
        SesionUsuario.exigirSesion();
        return repositorioLaboratorio.listarTodos();
    }

    /** Para los ComboBox: solo laboratorios activos. */
    public List<Laboratorio> listarActivos() {
        SesionUsuario.exigirSesion();
        return repositorioLaboratorio.listarTodos().stream()
                .filter(laboratorio -> laboratorio.getEstado() == EstadoRegistro.ACTIVO)
                .toList();
    }

    public Laboratorio registrar(Laboratorio laboratorio) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        normalizar(laboratorio);
        validarCampos(laboratorio);
        validarCodigoUnico(laboratorio, 0);

        laboratorio.setEstado(EstadoRegistro.ACTIVO);
        laboratorio.setIdLaboratorio(repositorioLaboratorio.insertar(laboratorio));
        return laboratorio;
    }

    public Laboratorio modificar(Laboratorio laboratorio) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        Laboratorio existente = repositorioLaboratorio.buscarPorId(laboratorio.getIdLaboratorio())
                .orElseThrow(() -> new BusinessException("El laboratorio que intenta modificar ya no existe."));
        normalizar(laboratorio);
        validarCampos(laboratorio);
        validarCodigoUnico(laboratorio, laboratorio.getIdLaboratorio());

        laboratorio.setEstado(existente.getEstado());
        repositorioLaboratorio.actualizar(laboratorio);
        return laboratorio;
    }

    /** Desactivación lógica: el laboratorio no se borra para conservar el historial de sus equipos. */
    public void cambiarEstado(int idLaboratorio, EstadoRegistro nuevoEstado) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN);
        Laboratorio existente = repositorioLaboratorio.buscarPorId(idLaboratorio)
                .orElseThrow(() -> new BusinessException("El laboratorio seleccionado ya no existe."));
        if (existente.getEstado() == nuevoEstado) {
            throw new BusinessException("El laboratorio " + existente.getCodigo()
                    + " ya está " + nuevoEstado.name().toLowerCase(Locale.ROOT) + ".");
        }
        repositorioLaboratorio.actualizarEstado(idLaboratorio, nuevoEstado);
    }

    private void normalizar(Laboratorio laboratorio) {
        String codigo = TextoUtil.limpiar(laboratorio.getCodigo());
        laboratorio.setCodigo(codigo == null ? null : codigo.toUpperCase(Locale.ROOT));
        laboratorio.setNombre(TextoUtil.limpiar(laboratorio.getNombre()));
        laboratorio.setUbicacion(TextoUtil.limpiar(laboratorio.getUbicacion()));
    }

    private void validarCampos(Laboratorio laboratorio) {
        if (laboratorio.getCodigo() == null) {
            throw new BusinessException("El código del laboratorio es obligatorio.");
        }
        if (laboratorio.getNombre() == null) {
            throw new BusinessException("El nombre del laboratorio es obligatorio.");
        }
        if (laboratorio.getUbicacion() == null) {
            throw new BusinessException("La ubicación del laboratorio es obligatoria.");
        }
        if (laboratorio.getCapacidad() <= 0) {
            throw new BusinessException("La capacidad debe ser un número mayor que cero.");
        }
        if (laboratorio.getCodigo().length() > 20) {
            throw new BusinessException("El código no puede superar 20 caracteres.");
        }
        if (laboratorio.getNombre().length() > 100) {
            throw new BusinessException("El nombre no puede superar 100 caracteres.");
        }
        if (laboratorio.getUbicacion().length() > 150) {
            throw new BusinessException("La ubicación no puede superar 150 caracteres.");
        }
    }

    /** RN-03: el código de laboratorio es único. */
    private void validarCodigoUnico(Laboratorio laboratorio, int idExcluido) {
        if (repositorioLaboratorio.existeCodigo(laboratorio.getCodigo(), idExcluido)) {
            throw new BusinessException("Ya existe un laboratorio con el código " + laboratorio.getCodigo() + ".");
        }
    }
}
