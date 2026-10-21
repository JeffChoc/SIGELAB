package universidad.sigelab.model;

import universidad.sigelab.enums.RolUsuario;

public class Rol {

    private int idRol;
    private RolUsuario nombre;
    private String descripcion;

    public Rol() {
    }

    public Rol(int idRol, RolUsuario nombre, String descripcion) {
        this.idRol = idRol;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }
    public RolUsuario getNombre() { return nombre; }
    public void setNombre(RolUsuario nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() {
        return nombre.name();
    }
}
