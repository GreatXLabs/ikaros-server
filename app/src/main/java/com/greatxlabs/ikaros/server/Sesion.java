package com.greatxlabs.ikaros.server;

public class Sesion {
    private int usuarioID;
    private String rol;
    private long ultimaActividad;

    public Sesion() {}

    public Sesion(int usuarioID, String rol) {
        this.usuarioID = usuarioID;
        this.rol = rol;
        this.ultimaActividad = System.currentTimeMillis();
    }

    public int getUsuarioID() { return usuarioID; }
    public void setUsuarioID(int usuarioID) { this.usuarioID = usuarioID; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public long getUltimaActividad() { return ultimaActividad; }
    public void setUltimaActividad(long ultimaActividad) { this.ultimaActividad = ultimaActividad; }

    public void renovar() {
        this.ultimaActividad = System.currentTimeMillis();
    }

    public boolean haExpirado() {
        long treintaMinutosEnMillis = 30 * 60 * 1000;
        return (System.currentTimeMillis() - ultimaActividad) > treintaMinutosEnMillis;
    }
}
