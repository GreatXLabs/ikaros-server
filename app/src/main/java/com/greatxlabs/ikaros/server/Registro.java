package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        this.fechaHora = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
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

    static class AccionJson {
        public int AccionID;
        public String Accion;
        public AccionJson() {}
    }

    static class EntidadJson {
        public int TipoEntidadID;
        public String TipoEntidad;
        public EntidadJson() {}
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    static final SemaforoRW registroLock = new SemaforoRW();

    static List<Registro> leerDesdeJson() {
        try {
            registroLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Registros.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<Registro>>() {});
            } finally {
                registroLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer Registros.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static void escribirEnJsonSinLock(List<Registro> registros) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Registros.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Registros", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), registros);
        Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING);
    }

    static List<AccionJson> leerAccionesDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Acciones.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<AccionJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Acciones.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static List<EntidadJson> leerEntidadesDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Entidades.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EntidadJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Entidades.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static String obtenerNombreAccionPorId(int accionID) {
        List<AccionJson> acciones = leerAccionesDesdeJson();
        for (AccionJson a : acciones) {
            if (a.AccionID == accionID) return a.Accion;
        }
        return null;
    }

    static String obtenerNombreEntidadPorId(int tipoEntidadID) {
        List<EntidadJson> entidades = leerEntidadesDesdeJson();
        for (EntidadJson e : entidades) {
            if (e.TipoEntidadID == tipoEntidadID) return e.TipoEntidad;
        }
        return null;
    }

    static void registrarLog(int usuarioID, int accionID, int tipoEntidadID, int entidadID, String descripcion) {
        try {
            registroLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Registros.json");
                List<Registro> registros = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Registro>>() {});

                int nuevoId = 1;
                for (Registro r : registros) {
                    if (r.getRegistroID() >= nuevoId) nuevoId = r.getRegistroID() + 1;
                }

                Registro nuevo = new Registro(nuevoId, accionID, usuarioID, tipoEntidadID, entidadID, descripcion);
                registros.add(nuevo);
                escribirEnJsonSinLock(registros);
            } finally {
                registroLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operacion interrumpida al registrar log: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al registrar log: " + e.getMessage());
        }
    }
}
