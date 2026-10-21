package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.enums.EstadoEquipo;
import universidad.sigelab.enums.ResultadoMantenimiento;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.enums.TipoMantenimiento;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Auditoria;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.Mantenimiento;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.repository.AuditoriaRepository;
import universidad.sigelab.repository.EquipoRepository;
import universidad.sigelab.repository.MantenimientoRepository;
import universidad.sigelab.util.SesionUsuario;
import universidad.sigelab.util.TextoUtil;
import universidad.sigelab.util.Transaccion;

import java.sql.Connection;

/**
 * Cambios de estado de un equipo ya registrado (sección 6.9 de la guía):
 *   DISPONIBLE -> DANADO          (reportar daño)
 *   DISPONIBLE -> MANTENIMIENTO   (preventivo)
 *   DANADO     -> MANTENIMIENTO   (correctivo)
 *   MANTENIMIENTO -> DISPONIBLE   (reparado)
 *   MANTENIMIENTO -> BAJA         (no reparable)
 * Cada cambio es una transacción: estado + historial de mantenimiento + auditoría, todo o nada.
 * PRESTADO no se toca aquí: solo cambia con préstamos y devoluciones (RN-09, RN-12, RN-13).
 */
public class EstadoEquipoService {

    private final EquipoRepository repositorioEquipo = new EquipoRepository();
    private final MantenimientoRepository repositorioMantenimiento = new MantenimientoRepository();
    private final AuditoriaRepository repositorioAuditoria = new AuditoriaRepository();

    public void reportarDanio(int idEquipo, String descripcionDanio) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        String descripcion = TextoUtil.limpiar(descripcionDanio);
        if (descripcion == null) {
            throw new BusinessException("Describa el daño del equipo.");
        }
        if (descripcion.length() > 250) {
            throw new BusinessException("La descripción del daño no puede superar 250 caracteres.");
        }
        Equipo equipo = obtenerEquipo(idEquipo);
        if (equipo.getEstado() != EstadoEquipo.DISPONIBLE) {
            throw new BusinessException("Solo un equipo DISPONIBLE puede reportarse como dañado. "
                    + "Estado actual: " + equipo.getEstado() + ".");
        }

        Transaccion.ejecutar(conexion -> {
            cambiarEstado(conexion, equipo, EstadoEquipo.DISPONIBLE, EstadoEquipo.DANADO);
            auditar(conexion, AccionAuditoria.EQUIPO_REPORTADO_DANADO, equipo,
                    "Daño reportado en " + equipo.getCodigoInventario() + ": " + descripcion);
        });
    }

    public void enviarAMantenimiento(int idEquipo, TipoMantenimiento tipo,
                                     String descripcionTrabajo, String responsable) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        String descripcion = TextoUtil.limpiar(descripcionTrabajo);
        String personaResponsable = TextoUtil.limpiar(responsable);
        if (tipo == null) {
            throw new BusinessException("Seleccione el tipo de mantenimiento.");
        }
        if (descripcion == null) {
            throw new BusinessException("La descripción del trabajo es obligatoria.");
        }
        if (personaResponsable == null) {
            throw new BusinessException("El responsable del mantenimiento es obligatorio.");
        }
        if (descripcion.length() > 300) {
            throw new BusinessException("La descripción no puede superar 300 caracteres.");
        }
        if (personaResponsable.length() > 100) {
            throw new BusinessException("El responsable no puede superar 100 caracteres.");
        }

        Equipo equipo = obtenerEquipo(idEquipo);
        EstadoEquipo estadoActual = equipo.getEstado();
        if (!estadoActual.permiteTransicionA(EstadoEquipo.MANTENIMIENTO)) {
            throw new BusinessException(mensajeNoPuedeIrAMantenimiento(equipo));
        }
        // DISPONIBLE solo va por mantenimiento preventivo; DANADO requiere correctivo.
        if (estadoActual == EstadoEquipo.DISPONIBLE && tipo != TipoMantenimiento.PREVENTIVO) {
            throw new BusinessException("Un equipo DISPONIBLE solo puede enviarse a mantenimiento preventivo.");
        }
        if (estadoActual == EstadoEquipo.DANADO && tipo != TipoMantenimiento.CORRECTIVO) {
            throw new BusinessException("Un equipo DANADO requiere mantenimiento correctivo.");
        }

        Usuario usuarioActual = SesionUsuario.getUsuarioActual();
        Transaccion.ejecutar(conexion -> {
            cambiarEstado(conexion, equipo, estadoActual, EstadoEquipo.MANTENIMIENTO);

            Mantenimiento mantenimiento = new Mantenimiento();
            mantenimiento.setIdEquipo(equipo.getIdEquipo());
            mantenimiento.setTipo(tipo);
            mantenimiento.setDescripcion(descripcion);
            mantenimiento.setResponsable(personaResponsable);
            mantenimiento.setIdUsuarioRegistra(usuarioActual.getIdUsuario());
            repositorioMantenimiento.insertar(conexion, mantenimiento);

            auditar(conexion, AccionAuditoria.EQUIPO_ENVIADO_MANTENIMIENTO, equipo,
                    "Equipo " + equipo.getCodigoInventario() + " enviado a mantenimiento " + tipo);
        });
    }

    /** REPARADO deja el equipo DISPONIBLE; NO_REPARABLE lo deja en BAJA (RN-15). */
    public void finalizarMantenimiento(int idEquipo, ResultadoMantenimiento resultado, String observaciones) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        if (resultado == null) {
            throw new BusinessException("Seleccione el resultado del mantenimiento.");
        }
        String observacionesLimpias = TextoUtil.limpiar(observaciones);
        if (observacionesLimpias != null && observacionesLimpias.length() > 300) {
            throw new BusinessException("Las observaciones no pueden superar 300 caracteres.");
        }
        Equipo equipo = obtenerEquipo(idEquipo);
        if (equipo.getEstado() != EstadoEquipo.MANTENIMIENTO) {
            throw new BusinessException("El equipo " + equipo.getCodigoInventario()
                    + " no está en mantenimiento. Estado actual: " + equipo.getEstado() + ".");
        }
        EstadoEquipo estadoFinal = resultado == ResultadoMantenimiento.REPARADO
                ? EstadoEquipo.DISPONIBLE : EstadoEquipo.BAJA;

        Transaccion.ejecutar(conexion -> {
            Mantenimiento abierto = repositorioMantenimiento.buscarAbiertoPorEquipo(conexion, equipo.getIdEquipo())
                    .orElseThrow(() -> new BusinessException("No se encontró un mantenimiento abierto para el equipo "
                            + equipo.getCodigoInventario() + "."));
            if (!repositorioMantenimiento.finalizar(conexion, abierto.getIdMantenimiento(),
                    resultado, observacionesLimpias)) {
                throw new BusinessException("El mantenimiento ya había sido cerrado. Actualice la pantalla.");
            }
            cambiarEstado(conexion, equipo, EstadoEquipo.MANTENIMIENTO, estadoFinal);
            auditar(conexion, AccionAuditoria.MANTENIMIENTO_FINALIZADO, equipo,
                    "Mantenimiento de " + equipo.getCodigoInventario() + " finalizado como " + resultado
                            + "; el equipo queda " + estadoFinal);
        });
    }

    // ---------- Privados ----------

    private Equipo obtenerEquipo(int idEquipo) {
        return repositorioEquipo.buscarPorId(idEquipo)
                .orElseThrow(() -> new BusinessException("El equipo seleccionado ya no existe."));
    }

    /** Solo cambia si el equipo sigue en el estado esperado: evita que dos operaciones se pisen. */
    private void cambiarEstado(Connection conexion, Equipo equipo,
                               EstadoEquipo estadoEsperado, EstadoEquipo estadoNuevo) {
        if (!repositorioEquipo.actualizarEstado(conexion, equipo.getIdEquipo(), estadoEsperado, estadoNuevo)) {
            throw new BusinessException("El equipo " + equipo.getCodigoInventario()
                    + " cambió de estado mientras se procesaba. Actualice la pantalla e intente de nuevo.");
        }
    }

    private void auditar(Connection conexion, AccionAuditoria accion, Equipo equipo, String descripcion) {
        Usuario usuario = SesionUsuario.getUsuarioActual();
        repositorioAuditoria.insertar(conexion,
                new Auditoria(usuario.getIdUsuario(), accion, "Equipo", equipo.getIdEquipo(), descripcion));
    }

    private String mensajeNoPuedeIrAMantenimiento(Equipo equipo) {
        switch (equipo.getEstado()) {
            case PRESTADO:
                return "El equipo " + equipo.getCodigoInventario()
                        + " está PRESTADO. Debe devolverse antes de enviarlo a mantenimiento.";
            case MANTENIMIENTO:
                return "El equipo " + equipo.getCodigoInventario() + " ya está en mantenimiento.";
            case BAJA:
                return "El equipo " + equipo.getCodigoInventario() + " está dado de baja.";
            default:
                return "El equipo " + equipo.getCodigoInventario()
                        + " no puede enviarse a mantenimiento desde el estado " + equipo.getEstado() + ".";
        }
    }
}
