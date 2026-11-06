package universidad.sigelab.service;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.enums.EstadoEquipo;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Auditoria;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.Prestamo;
import universidad.sigelab.model.PrestamoResumen;
import universidad.sigelab.model.Usuario;
import universidad.sigelab.repository.AuditoriaRepository;
import universidad.sigelab.repository.EquipoRepository;
import universidad.sigelab.repository.PrestamoRepository;
import universidad.sigelab.util.SesionUsuario;
import universidad.sigelab.util.TextoUtil;
import universidad.sigelab.util.Transaccion;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas del préstamo: RN-05, RN-06, RN-07, RN-08, RN-09, RN-14, RN-18, RN-19 y RN-20.
 * Confirmar un préstamo es UNA transacción: encabezado + detalle + estados + auditoría.
 */
public class PrestamoService {

    private final EquipoService servicioEquipo = new EquipoService();
    private final EquipoRepository repositorioEquipo = new EquipoRepository();
    private final PrestamoRepository repositorioPrestamo = new PrestamoRepository();
    private final AuditoriaRepository repositorioAuditoria = new AuditoriaRepository();

    /**
     * Identifica un equipo por su código (el que contiene el QR) y verifica si puede agregarse.
     * RN-19: identificar NO cambia ningún estado. Si el equipo existe pero no se puede prestar,
     * se informa por qué (CP-10: identificado, pero bloqueado).
     */
    public Equipo identificarParaPrestamo(String codigoInventario, List<Equipo> yaAgregados) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        Equipo equipo = servicioEquipo.buscarPorCodigo(codigoInventario);
        validarDisponible(equipo);
        for (Equipo agregado : yaAgregados) {
            if (agregado.getIdEquipo() == equipo.getIdEquipo()) { // RN-06
                throw new BusinessException("El equipo " + equipo.getCodigoInventario()
                        + " ya fue agregado a este préstamo.");
            }
        }
        return equipo;
    }

    /** Devuelve el id del préstamo creado. */
    public int confirmarPrestamo(String solicitante, String identificacion, LocalDateTime fechaEsperada,
                                 String observaciones, List<Equipo> equiposSeleccionados) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);

        String nombreSolicitante = TextoUtil.limpiar(solicitante);
        String identificacionSolicitante = TextoUtil.limpiar(identificacion);
        String observacionesLimpias = TextoUtil.limpiar(observaciones);
        validarDatosEncabezado(nombreSolicitante, identificacionSolicitante, observacionesLimpias, fechaEsperada);

        // RN-05: un préstamo debe tener al menos un equipo.
        if (equiposSeleccionados == null || equiposSeleccionados.isEmpty()) {
            throw new BusinessException("Agregue al menos un equipo al préstamo.");
        }

        // No se confía en lo que muestra la pantalla: se vuelve a consultar cada equipo en la BD.
        Set<Integer> idsVistos = new HashSet<>();
        List<Equipo> equiposActuales = new java.util.ArrayList<>();
        for (Equipo seleccionado : equiposSeleccionados) {
            if (!idsVistos.add(seleccionado.getIdEquipo())) { // RN-06
                throw new BusinessException("El equipo " + seleccionado.getCodigoInventario()
                        + " aparece más de una vez en el préstamo.");
            }
            Equipo actual = repositorioEquipo.buscarPorId(seleccionado.getIdEquipo())
                    .orElseThrow(() -> new BusinessException("El equipo "
                            + seleccionado.getCodigoInventario() + " ya no existe."));
            validarDisponible(actual); // RN-07, RN-08, RN-14
            equiposActuales.add(actual);
        }

        Usuario usuarioActual = SesionUsuario.getUsuarioActual();
        Prestamo prestamo = new Prestamo();
        prestamo.setSolicitanteNombre(nombreSolicitante);
        prestamo.setSolicitanteIdentificacion(identificacionSolicitante);
        prestamo.setFechaEsperadaDevolucion(fechaEsperada);
        prestamo.setIdUsuarioRegistra(usuarioActual.getIdUsuario());
        prestamo.setObservaciones(observacionesLimpias);

        final int[] idPrestamo = new int[1];
        Transaccion.ejecutar(conexion -> {
            idPrestamo[0] = repositorioPrestamo.insertar(conexion, prestamo);

            for (Equipo equipo : equiposActuales) {
                repositorioPrestamo.insertarDetalle(conexion, idPrestamo[0], equipo.getIdEquipo());
                // RN-09: al confirmar, cada equipo pasa a PRESTADO. Solo si sigue DISPONIBLE.
                boolean cambio = repositorioEquipo.actualizarEstado(conexion, equipo.getIdEquipo(),
                        EstadoEquipo.DISPONIBLE, EstadoEquipo.PRESTADO);
                if (!cambio) {
                    throw new BusinessException("El equipo " + equipo.getCodigoInventario()
                            + " dejó de estar disponible mientras se confirmaba. "
                            + "Se canceló el préstamo completo.");
                }
            }

            repositorioAuditoria.insertar(conexion, new Auditoria(usuarioActual.getIdUsuario(),
                    AccionAuditoria.PRESTAMO_CREADO, "Prestamo", idPrestamo[0],
                    descripcionAuditoria(nombreSolicitante, equiposActuales)));
        });
        return idPrestamo[0];
    }

    public List<PrestamoResumen> listarResumen() {
        SesionUsuario.exigirSesion();
        return repositorioPrestamo.listarResumen();
    }

    // ---------- Privados ----------

    /** RN-07 y RN-08: solo DISPONIBLE puede prestarse. */
    private void validarDisponible(Equipo equipo) {
        if (!equipo.getEstado().puedePrestarse()) {
            throw new BusinessException("El equipo " + equipo.getCodigoInventario()
                    + " no se puede prestar: su estado es " + equipo.getEstado() + ".");
        }
    }

    private void validarDatosEncabezado(String solicitante, String identificacion,
                                        String observaciones, LocalDateTime fechaEsperada) {
        if (solicitante == null) {
            throw new BusinessException("El nombre del solicitante es obligatorio.");
        }
        if (solicitante.length() > 120) {
            throw new BusinessException("El nombre del solicitante no puede superar 120 caracteres.");
        }
        if (identificacion != null && identificacion.length() > 30) {
            throw new BusinessException("La identificación no puede superar 30 caracteres.");
        }
        if (observaciones != null && observaciones.length() > 300) {
            throw new BusinessException("Las observaciones no pueden superar 300 caracteres.");
        }
        if (fechaEsperada == null) {
            throw new BusinessException("Indique la fecha esperada de devolución.");
        }
        if (!fechaEsperada.isAfter(LocalDateTime.now())) {
            throw new BusinessException("La fecha esperada de devolución debe ser posterior a la fecha actual.");
        }
    }

    private String descripcionAuditoria(String solicitante, List<Equipo> equipos) {
        StringBuilder codigos = new StringBuilder();
        for (Equipo equipo : equipos) {
            if (codigos.length() > 0) {
                codigos.append(", ");
            }
            codigos.append(equipo.getCodigoInventario());
        }
        String texto = "Préstamo a " + solicitante + " con " + equipos.size() + " equipo(s): " + codigos;
        return texto.length() > 300 ? texto.substring(0, 300) : texto;
    }
}
