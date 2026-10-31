package universidad.sigelab.model;

/** Una línea del préstamo: relaciona un equipo con su préstamo. */
public class DetallePrestamo {

    private int idDetalle;
    private int idPrestamo;
    private Equipo equipo;

    public DetallePrestamo() {
    }

    public DetallePrestamo(int idDetalle, int idPrestamo, Equipo equipo) {
        this.idDetalle = idDetalle;
        this.idPrestamo = idPrestamo;
        this.equipo = equipo;
    }

    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }
    public int getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(int idPrestamo) { this.idPrestamo = idPrestamo; }
    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }
}
