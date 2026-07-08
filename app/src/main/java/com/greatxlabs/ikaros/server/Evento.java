package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Evento {
    private int eventoID;
    private int misionID;
    private String titulo;
    private String fecha;
    private String descripcion;
    private int estadoEID;

    public Evento() {}

    @JsonProperty("EventoID")
    public int getEventoID() { return eventoID; }
    public void setEventoID(int eventoID) { this.eventoID = eventoID; }

    @JsonProperty("MisionID")
    public int getMisionID() { return misionID; }
    public void setMisionID(int misionID) { this.misionID = misionID; }

    @JsonProperty("Titulo")
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    @JsonProperty("Fecha")
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    @JsonProperty("Descripcion")
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @JsonProperty("EstadoEID")
    public int getEstadoEID() { return estadoEID; }
    public void setEstadoEID(int estadoEID) { this.estadoEID = estadoEID; }

    public void desestimar() {
        this.estadoEID = 2;
    }
}
