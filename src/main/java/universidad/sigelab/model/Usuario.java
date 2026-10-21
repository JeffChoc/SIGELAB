package universidad.sigelab.model;

import universidad.sigelab.enums.EstadoRegistro;

public class Usuario {

    private int idUsuario;
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private String contrasenaHash;
    private String correo;
    private Rol rol;
    private EstadoRegistro estado;
    private String referenciaFacial;

    public Usuario() {
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public EstadoRegistro getEstado() { return estado; }
    public void setEstado(EstadoRegistro estado) { this.estado = estado; }
    public String getReferenciaFacial() { return referenciaFacial; }
    public void setReferenciaFacial(String referenciaFacial) { this.referenciaFacial = referenciaFacial; }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    @Override
    public String toString() {
        return nombreUsuario + " (" + rol + ")";
    }
}
