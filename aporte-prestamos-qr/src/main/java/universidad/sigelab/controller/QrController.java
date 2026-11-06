package universidad.sigelab.controller;

import universidad.sigelab.service.QrService;

import java.io.File;

public class QrController {

    private final QrService servicioQr = new QrService();

    public void generarQr(String codigoInventario, File destino) {
        servicioQr.generarQrDeEquipo(codigoInventario, destino);
    }

    /** Devuelve el código de inventario leído. No cambia ningún estado (RN-19). */
    public String leerCodigo(File archivoImagen) {
        return servicioQr.leerCodigo(archivoImagen);
    }
}
