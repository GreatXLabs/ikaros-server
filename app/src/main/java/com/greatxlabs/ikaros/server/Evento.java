package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // --- Inner classes ---

    static class EstadoEvento {
        public int EstadoEID;
        public String Estado;
        public EstadoEvento() {}
    }

    // --- Infrastructure ---

    private static final ObjectMapper mapper = new ObjectMapper();
    static final SemaforoRW eventoLock = new SemaforoRW();

    private static String tsToString(Timestamp ts) {
        if (ts == null) return null;
        return ts.toLocalDateTime().toString();
    }

    // --- JSON I/O ---

    static List<Evento> leerDesdeJson() {
        try {
            eventoLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Eventos.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<Evento>>() {});
            } finally {
                eventoLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer Eventos.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static void escribirEnJsonSinLock(List<Evento> eventos) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Eventos.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Eventos", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), eventos);
        Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING);
    }

    static List<EstadoEvento> leerEstadosDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "EstadosEventos.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EstadoEvento>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer EstadosEventos.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- Lookups ---

    static String obtenerNombreEstadoPorId(int estadoEID) {
        List<EstadoEvento> estados = leerEstadosDesdeJson();
        for (EstadoEvento e : estados) {
            if (e.EstadoEID == estadoEID) return e.Estado;
        }
        return null;
    }

    // --- Map helpers ---

    static Map<String, Integer> obtenerEstadosComoMapa() {
        Map<String, Integer> mapa = new HashMap<>();
        List<EstadoEvento> estados = leerEstadosDesdeJson();
        for (EstadoEvento e : estados) {
            mapa.put(e.Estado.toUpperCase(), e.EstadoEID);
        }
        return mapa;
    }

    // --- CRUD ---

    static ResultSet listarEstados() {
        List<EstadoEvento> estados = leerEstadosDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (EstadoEvento e : estados) {
            filas.add(new String[]{String.valueOf(e.EstadoEID), e.Estado});
        }
        return new AccesoDatos.SimpleResultSet(filas, 2);
    }

    static int registrar(int usuarioIDLogueado, int misionID, String titulo, String desc, Timestamp fecha, AccesoDatos ad) {
        int nuevoId = -1;
        String descLog = null;
        try {
            eventoLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Eventos.json");
                List<Evento> eventos = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Evento>>() {});

                nuevoId = 1;
                for (Evento e : eventos) {
                    if (e.getEventoID() >= nuevoId) nuevoId = e.getEventoID() + 1;
                }

                Evento nuevo = new Evento();
                nuevo.setEventoID(nuevoId);
                nuevo.setMisionID(misionID);
                nuevo.setTitulo(titulo);
                nuevo.setFecha(tsToString(fecha));
                nuevo.setDescripcion(desc);
                nuevo.setEstadoEID(1);

                eventos.add(nuevo);
                escribirEnJsonSinLock(eventos);
                descLog = "MisionID=" + misionID + "|Titulo=" + titulo + "|Descripcion=" + desc;
            } finally {
                eventoLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        } catch (IOException e) {
            System.err.println("Error al registrar evento: " + e.getMessage());
            return -1;
        }
        if (descLog != null) {
            ad.registrarLog(usuarioIDLogueado, 6, 3, nuevoId, descLog);
        }
        return nuevoId;
    }

    static void baja(int usuarioIDLogueado, int eventoID, AccesoDatos ad) {
        String desc = null;
        try {
            eventoLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Eventos.json");
                List<Evento> eventos = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Evento>>() {});
                boolean encontrado = false;
                for (Evento e : eventos) {
                    if (e.getEventoID() == eventoID) {
                        String estadoAnterior = obtenerNombreEstadoPorId(e.getEstadoEID());
                        e.desestimar();
                        String estadoActual = obtenerNombreEstadoPorId(e.getEstadoEID());
                        escribirEnJsonSinLock(eventos);
                        desc = "Estado:" + estadoAnterior + "->" + estadoActual;
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado) {
                    escribirEnJsonSinLock(eventos);
                }
            } finally {
                eventoLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operacion interrumpida al dar de baja evento: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al desestimar evento: " + e.getMessage());
        }
        if (desc != null) {
            ad.registrarLog(usuarioIDLogueado, 7, 3, eventoID, desc);
        }
    }

    static ResultSet listar() {
        List<Evento> eventos = leerDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (Evento e : eventos) {
            filas.add(new String[]{
                String.valueOf(e.getEventoID()),
                Mision.obtenerNombrePorId(e.getMisionID()),
                e.getTitulo() != null ? e.getTitulo() : "",
                e.getFecha() != null ? e.getFecha() : "",
                e.getDescripcion() != null ? e.getDescripcion() : "",
                obtenerNombreEstadoPorId(e.getEstadoEID())
            });
        }
        return new AccesoDatos.SimpleResultSet(filas, 6);
    }

    static ResultSet consultarPorMision(int misionID) {
        List<Evento> eventos = leerDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (Evento e : eventos) {
            if (e.getMisionID() == misionID) {
                filas.add(new String[]{
                    String.valueOf(e.getEventoID()),
                    e.getTitulo() != null ? e.getTitulo() : "",
                    e.getFecha() != null ? e.getFecha() : "",
                    e.getDescripcion() != null ? e.getDescripcion() : "",
                    obtenerNombreEstadoPorId(e.getEstadoEID())
                });
            }
        }
        return new AccesoDatos.SimpleResultSet(filas, 5);
    }
}
