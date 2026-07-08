package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Mision {
    private int misionID;
    private int estadoMID;
    private Integer retrasoInicio;
    private String fechaInicioEstimada;
    private String fechaFinEstimada;
    private Integer retrasoFin;
    private String nombre;
    private String descripcion;

    public Mision() {}

    @JsonProperty("MisionID")
    public int getMisionID() { return misionID; }
    public void setMisionID(int misionID) { this.misionID = misionID; }

    @JsonProperty("EstadoMID")
    public int getEstadoMID() { return estadoMID; }
    public void setEstadoMID(int estadoMID) { this.estadoMID = estadoMID; }

    @JsonProperty("RetrasoInicio")
    public Integer getRetrasoInicio() { return retrasoInicio; }
    public void setRetrasoInicio(Integer retrasoInicio) { this.retrasoInicio = retrasoInicio; }

    @JsonProperty("FechaInicioEstimada")
    public String getFechaInicioEstimada() { return fechaInicioEstimada; }
    public void setFechaInicioEstimada(String fechaInicioEstimada) { this.fechaInicioEstimada = fechaInicioEstimada; }

    @JsonProperty("FechaFinEstimada")
    public String getFechaFinEstimada() { return fechaFinEstimada; }
    public void setFechaFinEstimada(String fechaFinEstimada) { this.fechaFinEstimada = fechaFinEstimada; }

    @JsonProperty("RetrasoFin")
    public Integer getRetrasoFin() { return retrasoFin; }
    public void setRetrasoFin(Integer retrasoFin) { this.retrasoFin = retrasoFin; }

    @JsonProperty("Nombre")
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @JsonProperty("Descripcion")
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean estaTerminada() {
        return estadoMID == 4 || estadoMID == 5;
    }
}
