
package universidad.sigelab.model;

import java.util.List;

/**
 * Resultado genérico de un reporte:
 * títulos de columna y filas listas para mostrar.
 */
public class ResultadoReporte {

    private final List<String> columnas;
    private final List<Object[]> filas;

    public ResultadoReporte(
            List<String> columnas,
            List<Object[]> filas) {

        this.columnas = columnas;
        this.filas = filas;
    }

    public List<String> getColumnas() {
        return columnas;
    }

    public List<Object[]> getFilas() {
        return filas;
    }
}
