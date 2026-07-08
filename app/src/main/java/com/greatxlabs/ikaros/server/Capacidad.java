package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Capacidad {
    private int tripulanteID;
    private int aptitudID;
    private int calificacion;
    private String fechaCapacidades;

    public Capacidad() {}

    @JsonProperty("TripulanteID")
    public int getTripulanteID() { return tripulanteID; }
    public void setTripulanteID(int tripulanteID) { this.tripulanteID = tripulanteID; }

    @JsonProperty("AptitudID")
    public int getAptitudID() { return aptitudID; }
    public void setAptitudID(int aptitudID) { this.aptitudID = aptitudID; }

    @JsonProperty("Calificacion")
    public int getCalificacion() { return calificacion; }
    public void setCalificacion(int calificacion) { this.calificacion = calificacion; }

    @JsonProperty("FechaCapacidades")
    public String getFechaCapacidades() { return fechaCapacidades; }
    public void setFechaCapacidades(String fechaCapacidades) { this.fechaCapacidades = fechaCapacidades; }
}
