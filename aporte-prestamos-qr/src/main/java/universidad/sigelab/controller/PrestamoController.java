package universidad.sigelab.controller;

import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.PrestamoResumen;
import universidad.sigelab.service.PrestamoService;

import java.time.LocalDateTime;
import java.util.List;

public class PrestamoController {

    private final PrestamoService servicioPrestamo = new PrestamoService();

    public Equipo identificarEquipo(String codigoInventario, List<Equipo> yaAgregados) {
        return servicioPrestamo.identificarParaPrestamo(codigoInventario, yaAgregados);
    }

    public int confirmarPrestamo(String solicitante, String identificacion, LocalDateTime fechaEsperada,
                                 String observaciones, List<Equipo> equipos) {
        return servicioPrestamo.confirmarPrestamo(solicitante, identificacion, fechaEsperada,
                observaciones, equipos);
    }

    public List<PrestamoResumen> listarResumen() {
        return servicioPrestamo.listarResumen();
    }
}
