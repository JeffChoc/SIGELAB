package universidad.sigelab.model;

/** Cantidades que muestra el formulario principal (sección 6.11 de la guía). */
public class DashboardResumen {

    private final int totalEquipos;
    private final int disponibles;
    private final int prestados;
    private final int danados;
    private final int enMantenimiento;
    private final int deBaja;
    private final int prestamosActivos;
    private final int prestamosVencidos;

    public DashboardResumen(int totalEquipos, int disponibles, int prestados, int danados,
                            int enMantenimiento, int deBaja, int prestamosActivos, int prestamosVencidos) {
        this.totalEquipos = totalEquipos;
        this.disponibles = disponibles;
        this.prestados = prestados;
        this.danados = danados;
        this.enMantenimiento = enMantenimiento;
        this.deBaja = deBaja;
        this.prestamosActivos = prestamosActivos;
        this.prestamosVencidos = prestamosVencidos;
    }

    public int getTotalEquipos() { return totalEquipos; }
    public int getDisponibles() { return disponibles; }
    public int getPrestados() { return prestados; }
    public int getDanados() { return danados; }
    public int getEnMantenimiento() { return enMantenimiento; }
    public int getDeBaja() { return deBaja; }
    public int getPrestamosActivos() { return prestamosActivos; }
    public int getPrestamosVencidos() { return prestamosVencidos; }
}
