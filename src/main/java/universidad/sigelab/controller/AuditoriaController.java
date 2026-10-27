package universidad.sigelab.controller;

import universidad.sigelab.enums.AccionAuditoria;
import universidad.sigelab.model.ResultadoReporte;
import universidad.sigelab.service.AuditoriaService;

import java.time.LocalDate;

/**
 * Controlador de consultas de auditoría.
 */
public class AuditoriaController {

    private final AuditoriaService servicioAuditoria =
            new AuditoriaService();

    /**
     * Consulta los registros de auditoría.
     *
     * Los filtros son opcionales:
     * null significa que no se filtra por ese dato.
     */
    public ResultadoReporte consultar(
            LocalDate desde,
            LocalDate hasta,
            AccionAuditoria accion,
            String usuario) {

        return servicioAuditoria.generarReporte(
                desde,
                hasta,
                accion,
                usuario
        );
    }
}
