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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    static class AptitudJson {
        public int AptitudID;
        public String Aptitud;
        public AptitudJson() {}
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    static final SemaforoRW capacidadLock = new SemaforoRW();

    static List<Capacidad> leerDesdeJson() {
        try {
            capacidadLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<Capacidad>>() {});
            } finally {
                capacidadLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer Capacidades.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static void escribirEnJsonSinLock(List<Capacidad> capacidades) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Capacidades", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), capacidades);
        Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING);
    }

    static List<AptitudJson> leerAptitudesDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Aptitudes.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<AptitudJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Aptitudes.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static String obtenerNombreAptitudPorId(int aptitudID) {
        List<AptitudJson> aptitudes = leerAptitudesDesdeJson();
        for (AptitudJson a : aptitudes) {
            if (a.AptitudID == aptitudID) return a.Aptitud;
        }
        return null;
    }

    static Map<String, Integer> obtenerAptitudesComoMapa() {
        Map<String, Integer> mapa = new HashMap<>();
        List<AptitudJson> aptitudes = leerAptitudesDesdeJson();
        for (AptitudJson a : aptitudes) {
            mapa.put(a.Aptitud.toUpperCase(), a.AptitudID);
        }
        return mapa;
    }

    static ResultSet consultar(int tripulanteID) {
        List<Capacidad> capacidades = leerDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (Capacidad c : capacidades) {
            if (c.getTripulanteID() == tripulanteID) {
                filas.add(new String[]{
                    String.valueOf(c.getAptitudID()),
                    obtenerNombreAptitudPorId(c.getAptitudID()),
                    String.valueOf(c.getCalificacion()),
                    c.getFechaCapacidades() != null ? c.getFechaCapacidades() : ""
                });
            }
        }
        return new AccesoDatos.SimpleResultSet(filas, 4);
    }

    static void registrar(int usuarioIDLogueado, int tripulanteID, int aptitudID, int calificacion, String fecha, AccesoDatos ad) {
        String desc = null;
        try {
            capacidadLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                List<Capacidad> capacidades = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Capacidad>>() {});

                for (Capacidad c : capacidades) {
                    if (c.getTripulanteID() == tripulanteID && c.getAptitudID() == aptitudID) {
                        c.setCalificacion(calificacion);
                        c.setFechaCapacidades(fecha);
                        escribirEnJsonSinLock(capacidades);
                        return;
                    }
                }

                Capacidad nueva = new Capacidad();
                nueva.setTripulanteID(tripulanteID);
                nueva.setAptitudID(aptitudID);
                nueva.setCalificacion(calificacion);
                nueva.setFechaCapacidades(fecha);
                capacidades.add(nueva);
                escribirEnJsonSinLock(capacidades);

                String nombreAptitud = obtenerNombreAptitudPorId(aptitudID);
                String nombreTripulante = Tripulante.obtenerNombreCompletoPorId(tripulanteID);
                desc = "Aptitud=" + (nombreAptitud != null ? nombreAptitud : aptitudID)
                        + "|Tripulante=" + (nombreTripulante != null ? nombreTripulante : tripulanteID)
                        + "|Calificacion=" + calificacion
                        + "|Fecha=" + (fecha != null ? fecha : "");
            } finally {
                capacidadLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operacion interrumpida al registrar capacidad: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al registrar capacidad: " + e.getMessage());
        }
        if (desc != null) {
            ad.registrarLog(usuarioIDLogueado, 11, 2, tripulanteID, desc);
        }
    }

    static void eliminar(int tripulanteID) {
        try {
            capacidadLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                List<Capacidad> capacidades = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Capacidad>>() {});

                capacidades.removeIf(c -> c.getTripulanteID() == tripulanteID);
                escribirEnJsonSinLock(capacidades);
            } finally {
                capacidadLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operacion interrumpida al eliminar capacidades: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al eliminar capacidades: " + e.getMessage());
        }
    }

    static ResultSet consultarAptitudes() {
        List<AptitudJson> aptitudes = leerAptitudesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (AptitudJson a : aptitudes) {
            filas.add(new String[]{String.valueOf(a.AptitudID), a.Aptitud});
        }
        return new AccesoDatos.SimpleResultSet(filas, 2);
    }
}
