package universidad.sigelab.model;

import universidad.sigelab.enums.EstadoEquipo;

public class Equipo {

    private int idEquipo;
    private String codigoInventario;
    private String nombre;
    private TipoEquipo tipoEquipo;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private Laboratorio laboratorio;
    private EstadoEquipo estado;
    private String observaciones;

    public Equipo() {
    }

    public Equipo(int idEquipo, String codigoInventario, String nombre, TipoEquipo tipoEquipo,
                  String marca, String modelo, String numeroSerie, Laboratorio laboratorio,
                  EstadoEquipo estado, String observaciones) {
        this.idEquipo = idEquipo;
        this.codigoInventario = codigoInventario;
        this.nombre = nombre;
        this.tipoEquipo = tipoEquipo;
        this.marca = marca;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.laboratorio = laboratorio;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public int getIdEquipo() { return idEquipo; }
    public void setIdEquipo(int idEquipo) { this.idEquipo = idEquipo; }
    public String getCodigoInventario() { return codigoInventario; }
    public void setCodigoInventario(String codigoInventario) { this.codigoInventario = codigoInventario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoEquipo getTipoEquipo() { return tipoEquipo; }
    public void setTipoEquipo(TipoEquipo tipoEquipo) { this.tipoEquipo = tipoEquipo; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public Laboratorio getLaboratorio() { return laboratorio; }
    public void setLaboratorio(Laboratorio laboratorio) { this.laboratorio = laboratorio; }
    public EstadoEquipo getEstado() { return estado; }
    public void setEstado(EstadoEquipo estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    @Override
    public String toString() {
        return codigoInventario + " - " + nombre;
    }
}
