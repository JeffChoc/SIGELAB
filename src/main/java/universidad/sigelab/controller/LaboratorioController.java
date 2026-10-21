package universidad.sigelab.controller;

import universidad.sigelab.enums.EstadoRegistro;
import universidad.sigelab.exception.BusinessException;
import universidad.sigelab.model.Laboratorio;
import universidad.sigelab.service.LaboratorioService;

import java.util.List;

public class LaboratorioController {

    private final LaboratorioService servicioLaboratorio = new LaboratorioService();

    public List<Laboratorio> listar() {
        return servicioLaboratorio.listar();
    }

    public Laboratorio registrar(String codigo, String nombre, String ubicacion, String capacidadTexto) {
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setCodigo(codigo);
        laboratorio.setNombre(nombre);
        laboratorio.setUbicacion(ubicacion);
        laboratorio.setCapacidad(convertirCapacidad(capacidadTexto));
        return servicioLaboratorio.registrar(laboratorio);
    }

    public Laboratorio modificar(int idLaboratorio, String codigo, String nombre,
                                 String ubicacion, String capacidadTexto) {
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setIdLaboratorio(idLaboratorio);
        laboratorio.setCodigo(codigo);
        laboratorio.setNombre(nombre);
        laboratorio.setUbicacion(ubicacion);
        laboratorio.setCapacidad(convertirCapacidad(capacidadTexto));
        return servicioLaboratorio.modificar(laboratorio);
    }

    public void cambiarEstado(int idLaboratorio, EstadoRegistro nuevoEstado) {
        servicioLaboratorio.cambiarEstado(idLaboratorio, nuevoEstado);
    }

    /** Convierte el texto del formulario; la regla (mayor que cero) la valida el Service. */
    private int convertirCapacidad(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new BusinessException("La capacidad es obligatoria.");
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("La capacidad debe ser un número entero.");
        }
    }
}
