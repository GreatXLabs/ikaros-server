package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Usuario {
    private int usuarioID;
    private int rolID;
    private int estadoUID;
    private String nombre;
    private String apellido;
    private String usuario;
    private String clave;

    public Usuario() {}

    @JsonProperty("UsuarioID")
    public int getUsuarioID() { return usuarioID; }
    public void setUsuarioID(int usuarioID) { this.usuarioID = usuarioID; }

    @JsonProperty("RolID")
    public int getRolID() { return rolID; }
    public void setRolID(int rolID) { this.rolID = rolID; }

    @JsonProperty("EstadoUID")
    public int getEstadoUID() { return estadoUID; }
    public void setEstadoUID(int estadoUID) { this.estadoUID = estadoUID; }

    @JsonProperty("Nombre")
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @JsonProperty("Apellido")
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    @JsonProperty("Usuario")
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    @JsonProperty("Clave")
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public boolean estaInactivo() {
        return estadoUID == 2;
    }

    public void desactivar() {
        this.estadoUID = 2;
    }
}
