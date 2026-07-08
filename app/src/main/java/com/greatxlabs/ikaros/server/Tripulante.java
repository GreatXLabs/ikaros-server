package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tripulante {
    private int tripulanteID;
    private int estadoTID;
    private int sexoID;
    private int peso;
    private int altura;
    private String nombre;
    private String apellido;
    private String imagen;
    private String fechaDeNacimiento;

    public Tripulante() {}

    @JsonProperty("TripulanteID")
    public int getTripulanteID() { return tripulanteID; }
    public void setTripulanteID(int tripulanteID) { this.tripulanteID = tripulanteID; }

    @JsonProperty("EstadoTID")
    public int getEstadoTID() { return estadoTID; }
    public void setEstadoTID(int estadoTID) { this.estadoTID = estadoTID; }

    @JsonProperty("SexoID")
    public int getSexoID() { return sexoID; }
    public void setSexoID(int sexoID) { this.sexoID = sexoID; }

    @JsonProperty("Peso")
    public int getPeso() { return peso; }
    public void setPeso(int peso) { this.peso = peso; }

    @JsonProperty("Altura")
    public int getAltura() { return altura; }
    public void setAltura(int altura) { this.altura = altura; }

    @JsonProperty("Nombre")
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @JsonProperty("Apellido")
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    @JsonProperty("Imagen")
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    @JsonProperty("FechaDeNacimiento")
    public String getFechaDeNacimiento() { return fechaDeNacimiento; }
    public void setFechaDeNacimiento(String fechaDeNacimiento) { this.fechaDeNacimiento = fechaDeNacimiento; }

    public boolean estaRetirado() {
        return estadoTID == 3;
    }

    public void retirar() {
        this.estadoTID = 3;
    }
}
