package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.enums.CondicionDevolucion;
import universidad.sigelab.enums.EstadoEquipo;
import universidad.sigelab.enums.EstadoPrestamo;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Auditoria;
import universidad.sigelab.model.DetallePrestamo;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.Prestamo;
import universidad.sigelab.model.PrestamoResumen;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.repository.AuditoriaRepository;
import universidad.sigelab.repository.DevolucionRepository;
import universidad.sigelab.repository.EquipoRepository;
import universidad.sigelab.repository.PrestamoRepository;
import universidad.sigelab.util.SesionUsuario;
import universidad.sigelab.util.TextoUtil;
import universidad.sigelab.util.Transaccion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reglas de devolución: RN-10, RN-11, RN-12, RN-13, RN-16, RN-18 y RN-20.
 * Una devolución es UNA transacción: devoluciones + estados de equipo + cierre del préstamo + auditoría.
 */
public class DevolucionService {

    private final PrestamoRepository repositorioPrestamo = new PrestamoRepository();
    private final DevolucionRepository repositorioDevolucion = new DevolucionRepository();
    private final EquipoRepository repositorioEquipo = new EquipoRepository();
    private final AuditoriaRepository repositorioAuditoria = new AuditoriaRepository();

    public List<PrestamoResumen> listarPrestamosActivos() {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        List<PrestamoResumen> activos = new ArrayList<>();
        for (PrestamoResumen resumen : repositorioPrestamo.listarResumen()) {
            if (resumen.getEstado() == EstadoPrestamo.ACTIVO) {
                activos.add(resumen);
            }
        }
        return activos;
    }

    public List<DetallePrestamo> listarPendientes(int idPrestamo) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        return repositorioDevolucion.listarPendientes(idPrestamo);
    }

    /** Permite buscar el préstamo a partir del equipo que se está devolviendo (por ejemplo, con su QR). */
    public int buscarPrestamoPorCodigoEquipo(String codigoInventario) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        String codigo = TextoUtil.limpiar(codigoInventario);
        if (codigo == null) {
            throw new BusinessException("Ingrese un código de inventario.");
        }
        String codigoNormalizado = codigo.toUpperCase(Locale.ROOT);
        return repositorioDevolucion.buscarPrestamoPendientePorCodigo(codigoNormalizado)
                .orElseThrow(() -> new BusinessException("El equipo " + codigoNormalizado
                        + " no está pendiente de devolución en ningún préstamo activo."));
    }

    /**
     * Registra la devolución de uno o varios equipos del préstamo con la misma condición.
     * Devuelve cuántos equipos quedan pendientes; 0 significa que el préstamo quedó FINALIZADO.
     */
    public int registrarDevolucion(int idPrestamo, List<Integer> idsDetalle,
                                   CondicionDevolucion condicion, String observaciones) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);

        if (idsDetalle == null || idsDetalle.isEmpty()) {
            throw new BusinessException("Seleccione al menos un equipo para devolver.");
        }
        if (condicion == null) {
            throw new BusinessException("Seleccione la condición en que regresa el equipo.");
        }
        String observacionesLimpias = TextoUtil.limpiar(observaciones);
        if (observacionesLimpias != null && observacionesLimpias.length() > 300) {
            throw new BusinessException("Las observaciones no pueden superar 300 caracteres.");
        }

        // RN-10: el préstamo debe existir y seguir activo.
        Prestamo prestamo = repositorioPrestamo.buscarPorId(idPrestamo)
                .orElseThrow(() -> new BusinessException("El préstamo seleccionado ya no existe."));
        if (prestamo.getEstado() != EstadoPrestamo.ACTIVO) {
            throw new BusinessException("El préstamo N.º " + idPrestamo + " ya está finalizado.");
        }

        // RN-10 y RN-11: solo se devuelve lo que sigue pendiente dentro de este préstamo.
        Map<Integer, DetallePrestamo> pendientesPorId = new HashMap<>();
        for (DetallePrestamo pendiente : repositorioDevolucion.listarPendientes(idPrestamo)) {
            pendientesPorId.put(pendiente.getIdDetalle(), pendiente);
        }
        Set<Integer> idsUnicos = new LinkedHashSet<>(idsDetalle);
        List<DetallePrestamo> aDevolver = new ArrayList<>();
        for (Integer idDetalle : idsUnicos) {
            DetallePrestamo detalle = pendientesPorId.get(idDetalle);
            if (detalle == null) {
                throw new BusinessException("Uno de los equipos seleccionados ya fue devuelto "
                        + "o no pertenece a este préstamo.");
            }
            aDevolver.add(detalle);
        }

        // RN-12: BUENO -> DISPONIBLE. RN-13: DANADO -> DANADO.
        EstadoEquipo estadoNuevo = condicion == CondicionDevolucion.BUENO
                ? EstadoEquipo.DISPONIBLE : EstadoEquipo.DANADO;
        Usuario usuarioActual = SesionUsuario.getUsuarioActual();

        final int[] pendientesRestantes = new int[1];
        Transaccion.ejecutar(conexion -> {
            for (DetallePrestamo detalle : aDevolver) {
                Equipo equipo = detalle.getEquipo();
                repositorioDevolucion.insertar(conexion, detalle.getIdDetalle(), condicion,
                        observacionesLimpias, usuarioActual.getIdUsuario());

                boolean cambio = repositorioEquipo.actualizarEstado(conexion, equipo.getIdEquipo(),
                        EstadoEquipo.PRESTADO, estadoNuevo);
                if (!cambio) {
                    throw new BusinessException("El equipo " + equipo.getCodigoInventario()
                            + " no está en estado PRESTADO. Se canceló la devolución completa.");
                }

                repositorioAuditoria.insertar(conexion, new Auditoria(usuarioActual.getIdUsuario(),
                        AccionAuditoria.EQUIPO_DEVUELTO, "Equipo", equipo.getIdEquipo(),
                        "Equipo " + equipo.getCodigoInventario() + " devuelto como " + condicion
                                + " (préstamo N.º " + idPrestamo + ")"));
            }

            // RN-16: el préstamo sigue activo mientras quede al menos un equipo pendiente.
            pendientesRestantes[0] = repositorioDevolucion.contarPendientes(conexion, idPrestamo);
            if (pendientesRestantes[0] == 0) {
                repositorioPrestamo.finalizar(conexion, idPrestamo);
            }
        });
        return pendientesRestantes[0];
    }
}
