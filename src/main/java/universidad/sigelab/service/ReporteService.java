package universidad.sigelab.service;

import universidad.sigelab.enums.FiltroPrestamos;
import universidad.sigelab.enums.RolUsuario;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.ResultadoReporte;
import universidad.sigelab.repository.ReporteRepository;
import universidad.sigelab.util.SesionUsuario;

import java.time.LocalDate;

/** Reportes y consultas. Solo ADMIN y ENCARGADO. */
public class ReporteService {

    private final ReporteRepository repositorioReporte = new ReporteRepository();
    private final EquipoService servicioEquipo = new EquipoService();

    public ResultadoReporte equiposPorEstado() {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        return repositorioReporte.equiposPorEstado();
    }

    public ResultadoReporte equiposPorLaboratorioYEstado() {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        return repositorioReporte.equiposPorLaboratorioYEstado();
    }

    public ResultadoReporte prestamos(FiltroPrestamos filtro) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        if (filtro == null) {
            throw new BusinessException("Seleccione el tipo de préstamos que desea consultar.");
        }
        switch (filtro) {
            case ACTIVOS:
                return repositorioReporte.prestamosActivos();
            case FINALIZADOS:
                return repositorioReporte.prestamosFinalizados();
            case VENCIDOS:
                return repositorioReporte.prestamosVencidos();
            default:
                return repositorioReporte.prestamosTodos();
        }
    }

    public ResultadoReporte prestamosEntreFechas(LocalDate desde, LocalDate hasta) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        if (desde == null || hasta == null) {
            throw new BusinessException("Indique las fechas desde y hasta.");
        }
        if (desde.isAfter(hasta)) {
            throw new BusinessException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        // BETWEEN incluye todo el día final: de las 00:00 del primer día a las 23:59:59 del último.
        return repositorioReporte.prestamosEntreFechas(desde.atStartOfDay(), hasta.atTime(23, 59, 59));
    }

    public ResultadoReporte historialPrestamosDeEquipo(String codigoInventario) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        Equipo equipo = servicioEquipo.buscarPorCodigo(codigoInventario); // rechaza si el equipo no existe
        return repositorioReporte.historialPrestamosDeEquipo(equipo.getCodigoInventario());
    }

    public ResultadoReporte historialMantenimientoDeEquipo(String codigoInventario) {
        SesionUsuario.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        Equipo equipo = servicioEquipo.buscarPorCodigo(codigoInventario);
        return repositorioReporte.historialMantenimientoDeEquipo(equipo.getCodigoInventario());
    }
}
