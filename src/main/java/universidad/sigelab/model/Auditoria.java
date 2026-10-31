package universidad.sigelab.model;

import universidad.sigelab.enums.AccionAuditoria;

import java.time.LocalDateTime;

public class Auditoria {

    private int idAuditoria;
    private Integer idUsuario;       // null en un LOGIN_FALLIDO con usuario inexistente
    private String nombreUsuario;    // se obtiene del JOIN con Usuario; puede ser null
    private LocalDateTime fechaHora;
    private AccionAuditoria accion;
    private String entidad;
    private Integer idRegistro;
    private String descripcion;

    public Auditoria() {
    }

    public Auditoria(Integer idUsuario, AccionAuditoria accion, String entidad,
                     Integer idRegistro, String descripcion) {
        this.idUsuario = idUsuario;
        this.accion = accion;
        this.entidad = entidad;
        this.idRegistro = idRegistro;
        this.descripcion = descripcion;
    }

    public int getIdAuditoria() { return idAuditoria; }
    public void setIdAuditoria(int idAuditoria) { this.idAuditoria = idAuditoria; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public AccionAuditoria getAccion() { return accion; }
    public void setAccion(AccionAuditoria accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public Integer getIdRegistro() { return idRegistro; }
    public void setIdRegistro(Integer idRegistro) { this.idRegistro = idRegistro; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
