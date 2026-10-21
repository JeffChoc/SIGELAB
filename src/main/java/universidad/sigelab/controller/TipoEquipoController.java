package universidad.sigelab.controller;

import universidad.sigelab.model.TipoEquipo;
import universidad.sigelab.service.TipoEquipoService;

import java.util.List;

public class TipoEquipoController {

    private final TipoEquipoService servicioTipoEquipo = new TipoEquipoService();

    public List<TipoEquipo> listar() {
        return servicioTipoEquipo.listar();
    }

    public TipoEquipo registrar(String nombre, String descripcion) {
        TipoEquipo tipo = new TipoEquipo();
        tipo.setNombre(nombre);
        tipo.setDescripcion(descripcion);
        return servicioTipoEquipo.registrar(tipo);
    }

    public TipoEquipo modificar(int idTipoEquipo, String nombre, String descripcion) {
        TipoEquipo tipo = new TipoEquipo();
        tipo.setIdTipoEquipo(idTipoEquipo);
        tipo.setNombre(nombre);
        tipo.setDescripcion(descripcion);
        return servicioTipoEquipo.modificar(tipo);
    }
}
