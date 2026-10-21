package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.enums.EstadoEquipo;
import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.Laboratorio;
import universidad.sigelab.model.TipoEquipo;
import universidad.sigelab.repository.EquipoRepository;
import universidad.sigelab.repository.LaboratorioRepository;
import universidad.sigelab.repository.TipoEquipoRepository;
import universidad.sigelab.util.SesionUsuario;

import java.util.List;
import java.util.Locale;

/** Aquí viven las reglas de negocio de Equipo (RN-04, RN-19 y validaciones del punto 9 de la guía). */
public class EquipoService {

    private final EquipoRepository repositorioEquipo = new EquipoRepository();
    private final LaboratorioRepository repositorioLaboratorio = new LaboratorioRepository();
    private final TipoEquipoRepository repositorioTipoEquipo = new TipoEquipoRepository();
    private final AuditoriaService servicioAuditoria = new AuditoriaService();

    // ---------- Consultas ----------

    public List<Equipo> listar() {
        SesionUsuario.exigirSesion();
        return repositorioEquipo.listarTodos();
    }

    public List<Equipo> listarPorEstado(EstadoEquipo estado) {
        SesionUsuario.exigirSesion();
        if (estado == null) {
            throw new BusinessException("Seleccione un estado válido.");
        }
        return repositorioEquipo.listarPorEstado(estado);
    }

    /**
     * Identifica un equipo por su código de inventario (el valor que contiene el QR).
     * RN-19: solo identifica, nunca modifica el estado.
     */
    public Equipo buscarPorCodigo(String codigoInventario) {
        SesionUsuario.exigirSesion();
        if (codigoInventario == null || codigoInventario.isBlank()) {
            throw new BusinessException("Ingrese un código de inventario.");
        }
        String codigo = codigoInventario.trim().toUpperCase(Locale.ROOT);
        return repositorioEquipo.buscarPorCodigo(codigo)
                .orElseThrow(() -> new BusinessException("No existe un equipo con el código " + codigo + "."));
    }

    // ---------- Registro y modificación ----------

    public Equipo registrar(Equipo equipo) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        normalizar(equipo);
        validarCampos(equipo);
        validarRelaciones(equipo);
        validarDuplicados(equipo, 0);

        validarEstadoInicial(equipo);

        int idGenerado = repositorioEquipo.insertar(equipo);
        equipo.setIdEquipo(idGenerado);
        servicioAuditoria.registrarComoUsuarioActual(AccionAuditoria.EQUIPO_REGISTRADO, "Equipo", idGenerado,
                "Se registró el equipo " + equipo.getCodigoInventario());
        return equipo;
    }

    public Equipo modificar(Equipo equipo) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        Equipo existente = repositorioEquipo.buscarPorId(equipo.getIdEquipo())
                .orElseThrow(() -> new BusinessException("El equipo que intenta modificar ya no existe."));
        if (existente.getEstado() == EstadoEquipo.BAJA) {
            throw new BusinessException("Un equipo dado de baja no puede modificarse.");
        }

        normalizar(equipo);
        validarCampos(equipo);
        validarRelaciones(equipo);
        validarDuplicados(equipo, equipo.getIdEquipo());

        // El estado nunca se cambia desde la edición del formulario.
        equipo.setEstado(existente.getEstado());

        repositorioEquipo.actualizar(equipo);
        servicioAuditoria.registrarComoUsuarioActual(AccionAuditoria.EQUIPO_MODIFICADO, "Equipo", equipo.getIdEquipo(),
                "Se modificó el equipo " + equipo.getCodigoInventario());
        return equipo;
    }

    // ---------- Validaciones privadas ----------

    /**
     * Un equipo nuevo puede registrarse DISPONIBLE (por defecto) o DANADO (si ya llegó con un problema).
     * PRESTADO solo nace al confirmar un préstamo (RN-09); MANTENIMIENTO y BAJA salen del módulo
     * de mantenimiento (RN-15). Por eso no se permiten al registrar.
     */
    private void validarEstadoInicial(Equipo equipo) {
        if (equipo.getEstado() == null) {
            equipo.setEstado(EstadoEquipo.DISPONIBLE);
            return;
        }
        if (equipo.getEstado() != EstadoEquipo.DISPONIBLE && equipo.getEstado() != EstadoEquipo.DANADO) {
            throw new BusinessException("Un equipo nuevo solo puede registrarse como DISPONIBLE o DANADO. "
                    + "Los demás estados se asignan con préstamos y mantenimiento.");
        }
    }

    private void normalizar(Equipo equipo) {
        equipo.setCodigoInventario(limpiar(equipo.getCodigoInventario()) == null
                ? null : limpiar(equipo.getCodigoInventario()).toUpperCase(Locale.ROOT));
        equipo.setNombre(limpiar(equipo.getNombre()));
        equipo.setMarca(limpiar(equipo.getMarca()));
        equipo.setModelo(limpiar(equipo.getModelo()));
        equipo.setNumeroSerie(limpiar(equipo.getNumeroSerie()));
        equipo.setObservaciones(limpiar(equipo.getObservaciones()));
    }

    /** Quita espacios; un texto vacío se guarda como null. */
    private String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private void validarCampos(Equipo equipo) {
        if (equipo.getCodigoInventario() == null) {
            throw new BusinessException("El código de inventario es obligatorio.");
        }
        if (equipo.getNombre() == null) {
            throw new BusinessException("El nombre del equipo es obligatorio.");
        }
        if (equipo.getTipoEquipo() == null) {
            throw new BusinessException("Seleccione el tipo de equipo.");
        }
        if (equipo.getLaboratorio() == null) {
            throw new BusinessException("Seleccione el laboratorio.");
        }
        validarLongitud(equipo.getCodigoInventario(), 30, "El código de inventario");
        validarLongitud(equipo.getNombre(), 100, "El nombre");
        validarLongitud(equipo.getMarca(), 60, "La marca");
        validarLongitud(equipo.getModelo(), 60, "El modelo");
        validarLongitud(equipo.getNumeroSerie(), 80, "El número de serie");
        validarLongitud(equipo.getObservaciones(), 300, "Las observaciones");
    }

    private void validarLongitud(String valor, int maximo, String campo) {
        if (valor != null && valor.length() > maximo) {
            throw new BusinessException(campo + " no puede superar " + maximo + " caracteres.");
        }
    }

    /** No se confía en el objeto que llega de la vista: se vuelve a consultar en la BD. */
    private void validarRelaciones(Equipo equipo) {
        TipoEquipo tipo = repositorioTipoEquipo.buscarPorId(equipo.getTipoEquipo().getIdTipoEquipo())
                .orElseThrow(() -> new BusinessException("El tipo de equipo seleccionado no existe."));
        Laboratorio laboratorio = repositorioLaboratorio.buscarPorId(equipo.getLaboratorio().getIdLaboratorio())
                .orElseThrow(() -> new BusinessException("El laboratorio seleccionado no existe."));
        if (laboratorio.getEstado() != EstadoRegistro.ACTIVO) {
            throw new BusinessException("El laboratorio " + laboratorio.getCodigo() + " está inactivo.");
        }
        equipo.setTipoEquipo(tipo);
        equipo.setLaboratorio(laboratorio);
    }

    /** RN-04: el código de inventario es único. */
    private void validarDuplicados(Equipo equipo, int idExcluido) {
        if (repositorioEquipo.existeCodigo(equipo.getCodigoInventario(), idExcluido)) {
            throw new BusinessException("Ya existe un equipo con el código " + equipo.getCodigoInventario() + ".");
        }
        if (equipo.getNumeroSerie() != null
                && repositorioEquipo.existeNumeroSerie(equipo.getNumeroSerie(), idExcluido)) {
            throw new BusinessException("Ya existe un equipo con el número de serie " + equipo.getNumeroSerie() + ".");
        }
    }
}
