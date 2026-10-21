package universidad.sigelab.model;

import universidad.sigelab.enums.EstadoMantenimiento;
import universidad.sigelab.enums.ResultadoMantenimiento;
import universidad.sigelab.enums.TipoMantenimiento;

import java.time.LocalDateTime;

public class Mantenimiento {

    private int idMantenimiento;
    private int idEquipo;
    private LocalDateTime fechaIngreso;
    private TipoMantenimiento tipo;
    private String descripcion;
    private String responsable;
    private LocalDateTime fechaSalida;          // null mientras esté EN_PROCESO
    private ResultadoMantenimiento resultado;   // null mientras esté EN_PROCESO
    private EstadoMantenimiento estado;
    private String observaciones;
    private int idUsuarioRegistra;

    public Mantenimiento() {
    }

    public int getIdMantenimiento() { return idMantenimiento; }
    public void setIdMantenimiento(int idMantenimiento) { this.idMantenimiento = idMantenimiento; }
    public int getIdEquipo() { return idEquipo; }
    public void setIdEquipo(int idEquipo) { this.idEquipo = idEquipo; }
    public LocalDateTime getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDateTime fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public TipoMantenimiento getTipo() { return tipo; }
    public void setTipo(TipoMantenimiento tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public LocalDateTime getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDateTime fechaSalida) { this.fechaSalida = fechaSalida; }
    public ResultadoMantenimiento getResultado() { return resultado; }
    public void setResultado(ResultadoMantenimiento resultado) { this.resultado = resultado; }
    public EstadoMantenimiento getEstado() { return estado; }
    public void setEstado(EstadoMantenimiento estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public int getIdUsuarioRegistra() { return idUsuarioRegistra; }
    public void setIdUsuarioRegistra(int idUsuarioRegistra) { this.idUsuarioRegistra = idUsuarioRegistra; }
}
