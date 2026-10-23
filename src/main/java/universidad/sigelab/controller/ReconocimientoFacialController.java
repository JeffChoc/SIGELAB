package universidad.sigelab.controller;

import universidad.sigelab.service.ReconocimientoFacialService;

public class ReconocimientoFacialController {

    private final ReconocimientoFacialService servicioFacial = new ReconocimientoFacialService();

    /** Abre la cámara y registra el rostro del usuario (solo ADMIN). Tarda unos segundos. */
    public void registrarRostro(int idUsuario) {
        servicioFacial.registrarRostro(idUsuario);
    }
}
