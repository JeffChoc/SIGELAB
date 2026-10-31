package universidad.sigelab.enums;

import java.util.Locale;

/** Coincide con los valores de la tabla Rol (columna nombre). */
public enum RolUsuario {
    ADMIN,
    ENCARGADO,
    DOCENTE;

    /** Convierte el nombre guardado en la base sin depender de mayúsculas/minúsculas. */
    public static RolUsuario desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol está vacío.");
        }
        return valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }

    public boolean esAdministrador() {
        return this == ADMIN;
    }

    /** ADMIN y ENCARGADO participan en la operación diaria del laboratorio. */
    public boolean puedeOperar() {
        return this == ADMIN || this == ENCARGADO;
    }
}
