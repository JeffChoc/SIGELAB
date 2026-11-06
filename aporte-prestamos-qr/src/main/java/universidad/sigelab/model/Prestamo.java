package universidad.sigelab.model;

import universidad.sigelab.enums.EstadoPrestamo;

import java.time.LocalDateTime;

/** Encabezado del préstamo. Los equipos se relacionan mediante la tabla DetallePrestamo. */
public class Prestamo {

    private int idPrestamo;
    private String solicitanteNombre;
    private String solicitanteIdentificacion;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaEsperadaDevolucion;
    private int idUsuarioRegistra;
    private EstadoPrestamo estado;
    private String observaciones;

    public Prestamo() {
    }

    public int getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(int idPrestamo) { this.idPrestamo = idPrestamo; }
    public String getSolicitanteNombre() { return solicitanteNombre; }
    public void setSolicitanteNombre(String solicitanteNombre) { this.solicitanteNombre = solicitanteNombre; }
    public String getSolicitanteIdentificacion() { return solicitanteIdentificacion; }
    public void setSolicitanteIdentificacion(String solicitanteIdentificacion) { this.solicitanteIdentificacion = solicitanteIdentificacion; }
    public LocalDateTime getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(LocalDateTime fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }
    public LocalDateTime getFechaEsperadaDevolucion() { return fechaEsperadaDevolucion; }
    public void setFechaEsperadaDevolucion(LocalDateTime fechaEsperadaDevolucion) { this.fechaEsperadaDevolucion = fechaEsperadaDevolucion; }
    public int getIdUsuarioRegistra() { return idUsuarioRegistra; }
    public void setIdUsuarioRegistra(int idUsuarioRegistra) { this.idUsuarioRegistra = idUsuarioRegistra; }
    public EstadoPrestamo getEstado() { return estado; }
    public void setEstado(EstadoPrestamo estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
