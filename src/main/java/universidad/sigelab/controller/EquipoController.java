package universidad.sigelab.controller;

import universidad.sigelab.enums.EstadoEquipo;
import universidad.sigelab.model.Equipo;
import universidad.sigelab.model.Laboratorio;
import universidad.sigelab.model.TipoEquipo;
import universidad.sigelab.service.EquipoService;
import universidad.sigelab.service.LaboratorioService;
import universidad.sigelab.service.TipoEquipoService;

import java.util.List;

/**
 * Coordina la vista con los servicios. No contiene reglas ni SQL.
 * Las excepciones se dejan subir: la vista las atrapa y usa ManejadorErrores.mensajeParaUsuario(e).
 */
public class EquipoController {

    private final EquipoService servicioEquipo = new EquipoService();
    private final LaboratorioService servicioLaboratorio = new LaboratorioService();
    private final TipoEquipoService servicioTipoEquipo = new TipoEquipoService();

    public List<Equipo> listar() {
        return servicioEquipo.listar();
    }

    public List<Equipo> listarPorEstado(EstadoEquipo estado) {
        return servicioEquipo.listarPorEstado(estado);
    }

    public Equipo buscarPorCodigo(String codigoInventario) {
        return servicioEquipo.buscarPorCodigo(codigoInventario);
    }

    /** Registra el equipo como DISPONIBLE (estado por defecto). */
    public Equipo registrar(String codigoInventario, String nombre, TipoEquipo tipoEquipo, String marca,
                            String modelo, String numeroSerie, Laboratorio laboratorio, String observaciones) {
        return registrar(codigoInventario, nombre, tipoEquipo, marca, modelo, numeroSerie,
                laboratorio, observaciones, null);
    }

    /** estadoInicial null = DISPONIBLE. El Service valida cuáles estados se permiten al registrar. */
    public Equipo registrar(String codigoInventario, String nombre, TipoEquipo tipoEquipo, String marca,
                            String modelo, String numeroSerie, Laboratorio laboratorio, String observaciones,
                            EstadoEquipo estadoInicial) {
        Equipo equipo = new Equipo();
        equipo.setCodigoInventario(codigoInventario);
        equipo.setNombre(nombre);
        equipo.setTipoEquipo(tipoEquipo);
        equipo.setMarca(marca);
        equipo.setModelo(modelo);
        equipo.setNumeroSerie(numeroSerie);
        equipo.setLaboratorio(laboratorio);
        equipo.setObservaciones(observaciones);
        equipo.setEstado(estadoInicial);
        return servicioEquipo.registrar(equipo);
    }

    public Equipo modificar(int idEquipo, String codigoInventario, String nombre, TipoEquipo tipoEquipo,
                            String marca, String modelo, String numeroSerie, Laboratorio laboratorio,
                            String observaciones) {
        Equipo equipo = new Equipo();
        equipo.setIdEquipo(idEquipo);
        equipo.setCodigoInventario(codigoInventario);
        equipo.setNombre(nombre);
        equipo.setTipoEquipo(tipoEquipo);
        equipo.setMarca(marca);
        equipo.setModelo(modelo);
        equipo.setNumeroSerie(numeroSerie);
        equipo.setLaboratorio(laboratorio);
        equipo.setObservaciones(observaciones);
        return servicioEquipo.modificar(equipo);
    }

    /** Datos para los ComboBox del formulario. */
    public List<TipoEquipo> tiposParaCombo() {
        return servicioTipoEquipo.listar();
    }

    public List<Laboratorio> laboratoriosParaCombo() {
        return servicioLaboratorio.listarActivos();
    }
}