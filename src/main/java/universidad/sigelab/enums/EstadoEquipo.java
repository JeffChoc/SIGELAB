package universidad.sigelab.enums;

public enum EstadoEquipo {
    DISPONIBLE,
    PRESTADO,
    DANADO,
    MANTENIMIENTO,
    BAJA;

    /** RN-07 y RN-08: solo un equipo DISPONIBLE puede prestarse. */
    public boolean puedePrestarse() {
        return this == DISPONIBLE;
    }

    /** Transiciones válidas según la guía (secciones 6.8 y 6.9). */
    public boolean permiteTransicionA(EstadoEquipo destino) {
        return switch (this) {
            case DISPONIBLE -> destino == PRESTADO || destino == MANTENIMIENTO;
            case PRESTADO -> destino == DISPONIBLE || destino == DANADO;
            case DANADO -> destino == MANTENIMIENTO;
            case MANTENIMIENTO -> destino == DISPONIBLE || destino == BAJA;
            case BAJA -> false;
        };
    }
}
