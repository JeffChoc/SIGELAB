package universidad.sigelab.model;

import universidad.sigelab.enums.EstadoPrestamo;

import java.time.LocalDateTime;

/** Fila para listados y reportes: el préstamo con sus conteos de equipos. */
public class PrestamoResumen {

    private final int idPrestamo;
    private final String solicitanteNombre;
    private final LocalDateTime fechaPrestamo;
    private final LocalDateTime fechaEsperadaDevolucion;
    private final EstadoPrestamo estado;
    private final int totalEquipos;
    private final int equiposPendientes;

    public PrestamoResumen(int idPrestamo, String solicitanteNombre, LocalDateTime fechaPrestamo,
                           LocalDateTime fechaEsperadaDevolucion, EstadoPrestamo estado,
                           int totalEquipos, int equiposPendientes) {
        this.idPrestamo = idPrestamo;
        this.solicitanteNombre = solicitanteNombre;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaEsperadaDevolucion = fechaEsperadaDevolucion;
        this.estado = estado;
        this.totalEquipos = totalEquipos;
        this.equiposPendientes = equiposPendientes;
    }

    public int getIdPrestamo() { return idPrestamo; }
    public String getSolicitanteNombre() { return solicitanteNombre; }
    public LocalDateTime getFechaPrestamo() { return fechaPrestamo; }
    public LocalDateTime getFechaEsperadaDevolucion() { return fechaEsperadaDevolucion; }
    public EstadoPrestamo getEstado() { return estado; }
    public int getTotalEquipos() { return totalEquipos; }
    public int getEquiposPendientes() { return equiposPendientes; }

    /** RN-17: vencido = la fecha esperada ya pasó y todavía hay equipos pendientes. */
    public boolean estaVencido() {
        return equiposPendientes > 0 && fechaEsperadaDevolucion.isBefore(LocalDateTime.now());
    }
}
