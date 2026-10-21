package universidad.sigelab.controller;

import universidad.sigelab.enums.FiltroPrestamos;
import universidad.sigelab.model.ResultadoReporte;
import universidad.sigelab.service.ReporteService;

import java.time.LocalDate;

public class ReporteController {

    private final ReporteService servicioReporte = new ReporteService();

    public ResultadoReporte equiposPorEstado() {
        return servicioReporte.equiposPorEstado();
    }

    public ResultadoReporte equiposPorLaboratorioYEstado() {
        return servicioReporte.equiposPorLaboratorioYEstado();
    }

    public ResultadoReporte prestamos(FiltroPrestamos filtro) {
        return servicioReporte.prestamos(filtro);
    }

    public ResultadoReporte prestamosEntreFechas(LocalDate desde, LocalDate hasta) {
        return servicioReporte.prestamosEntreFechas(desde, hasta);
    }

    public ResultadoReporte historialPrestamosDeEquipo(String codigoInventario) {
        return servicioReporte.historialPrestamosDeEquipo(codigoInventario);
    }

    public ResultadoReporte historialMantenimientoDeEquipo(String codigoInventario) {
        return servicioReporte.historialMantenimientoDeEquipo(codigoInventario);
    }
}
