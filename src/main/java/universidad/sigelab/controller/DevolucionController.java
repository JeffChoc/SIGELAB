package universidad.sigelab.controller;

import universidad.sigelab.enums.CondicionDevolucion;
import universidad.sigelab.model.DetallePrestamo;
import universidad.sigelab.model.PrestamoResumen;
import universidad.sigelab.service.DevolucionService;

import java.util.List;

public class DevolucionController {

    private final DevolucionService servicioDevolucion = new DevolucionService();

    public List<PrestamoResumen> listarPrestamosActivos() {
        return servicioDevolucion.listarPrestamosActivos();
    }

    public List<DetallePrestamo> listarPendientes(int idPrestamo) {
        return servicioDevolucion.listarPendientes(idPrestamo);
    }

    public int buscarPrestamoPorCodigoEquipo(String codigoInventario) {
        return servicioDevolucion.buscarPrestamoPorCodigoEquipo(codigoInventario);
    }

    /** Devuelve los equipos que quedan pendientes; 0 = el préstamo quedó finalizado. */
    public int registrarDevolucion(int idPrestamo, List<Integer> idsDetalle,
                                   CondicionDevolucion condicion, String observaciones) {
        return servicioDevolucion.registrarDevolucion(idPrestamo, idsDetalle, condicion, observaciones);
    }
}
