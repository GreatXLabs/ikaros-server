package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Registro {
    private int registroID;
    private int accionMID;
    private int usuarioID;
    private int tipoEntidadID;
    private int entidadID;
    private String fechaHora;
    private String descripcion;

    public Registro() {}

    public Registro(int registroID, int accionMID, int usuarioID, int tipoEntidadID, int entidadID, String descripcion) {
        this.registroID = registroID;
        this.accionMID = accionMID;
        this.usuarioID = usuarioID;
        this.tipoEntidadID = tipoEntidadID;
        this.entidadID = entidadID;
        this.fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.descripcion = descripcion != null ? descripcion : "";
    }

    @JsonProperty("RegistroID")
    public int getRegistroID() { return registroID; }
    public void setRegistroID(int registroID) { this.registroID = registroID; }

    @JsonProperty("AccionMID")
    public int getAccionMID() { return accionMID; }
    public void setAccionMID(int accionMID) { this.accionMID = accionMID; }

    @JsonProperty("UsuarioID")
    public int getUsuarioID() { return usuarioID; }
    public void setUsuarioID(int usuarioID) { this.usuarioID = usuarioID; }

    @JsonProperty("TipoEntidadID")
    public int getTipoEntidadID() { return tipoEntidadID; }
    public void setTipoEntidadID(int tipoEntidadID) { this.tipoEntidadID = tipoEntidadID; }

    @JsonProperty("EntidadID")
    public int getEntidadID() { return entidadID; }
    public void setEntidadID(int entidadID) { this.entidadID = entidadID; }

    @JsonProperty("FechaHora")
    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    @JsonProperty("Descripcion")
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
