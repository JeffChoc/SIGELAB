package universidad.sigelab.controller;

import universidad.sigelab.enums.ResultadoMantenimiento;
import universidad.sigelab.enums.TipoMantenimiento;
import universidad.sigelab.service.EstadoEquipoService;

public class EstadoEquipoController {

    private final EstadoEquipoService servicioEstadoEquipo = new EstadoEquipoService();

    public void reportarDanio(int idEquipo, String descripcionDanio) {
        servicioEstadoEquipo.reportarDanio(idEquipo, descripcionDanio);
    }

    public void enviarAMantenimiento(int idEquipo, TipoMantenimiento tipo,
                                     String descripcionTrabajo, String responsable) {
        servicioEstadoEquipo.enviarAMantenimiento(idEquipo, tipo, descripcionTrabajo, responsable);
    }

    public void finalizarMantenimiento(int idEquipo, ResultadoMantenimiento resultado, String observaciones) {
        servicioEstadoEquipo.finalizarMantenimiento(idEquipo, resultado, observaciones);
    }
}
