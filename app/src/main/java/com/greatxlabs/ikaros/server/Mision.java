package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // --- Inner classes ---

    static class EstadoMision {
        public int EstadoMID;
        public String Estado;
        public EstadoMision() {}
    }

    static class GrupoMision {
        public int TripulanteID;
        public int MisionID;
        public String FechaAsignacion;
        public GrupoMision() {}
    }

    // --- Infrastructure ---

    private static final ObjectMapper mapper = new ObjectMapper();
    static final SemaforoRW misionLock = new SemaforoRW();

    private static String tsToString(Timestamp ts) {
        if (ts == null) return null;
        return ts.toLocalDateTime().toString();
    }

    // --- JSON I/O ---

    static List<Mision> leerDesdeJson() {
        try {
            misionLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<Mision>>() {});
            } finally {
                misionLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer Misiones.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static void escribirEnJsonSinLock(List<Mision> misiones) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Misiones", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), misiones);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    static List<EstadoMision> leerEstadosDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "EstadosMisiones.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EstadoMision>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer EstadosMisiones.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static List<GrupoMision> leerGrupoMisionesDesdeJson() {
        try {
            misionLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "GrupoMisiones.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<GrupoMision>>() {});
            } finally {
                misionLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer GrupoMisiones.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static void escribirGrupoMisionesEnJsonSinLock(List<GrupoMision> grupos) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "GrupoMisiones.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "GrupoMisiones", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), grupos);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    // --- Lookups ---

    static String obtenerNombreEstadoPorId(int estadoMID) {
        List<EstadoMision> estados = leerEstadosDesdeJson();
        for (EstadoMision e : estados) {
            if (e.EstadoMID == estadoMID) return e.Estado;
        }
        return null;
    }

    static String obtenerNombrePorId(int misionID) {
        List<Mision> misiones = leerDesdeJson();
        for (Mision m : misiones) {
            if (m.getMisionID() == misionID) return m.getNombre();
        }
        return null;
    }

    // --- Map helpers ---

    static Map<String, Integer> obtenerEstadosComoMapa() {
        Map<String, Integer> mapa = new HashMap<>();
        List<EstadoMision> estados = leerEstadosDesdeJson();
        for (EstadoMision e : estados) {
            mapa.put(e.Estado.toUpperCase(), e.EstadoMID);
        }
        return mapa;
    }

    // --- CRUD ---

    static ResultSet listarEstados() throws SQLException {
        List<EstadoMision> estados = leerEstadosDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (EstadoMision e : estados) {
            filas.add(new String[]{String.valueOf(e.EstadoMID), e.Estado});
        }
        return new AccesoDatos.SimpleResultSet(filas, 2);
    }

    static int registrar(int estadoMID, String nombre, String descripcion, Timestamp ini, Timestamp fin) {
        try {
            misionLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
                List<Mision> misiones = mapper.readValue(ruta.toFile(), new TypeReference<List<Mision>>() {});

                int nuevoId = 1;
                for (Mision m : misiones) {
                    if (m.getMisionID() >= nuevoId) nuevoId = m.getMisionID() + 1;
                }

                Mision nueva = new Mision();
                nueva.setMisionID(nuevoId);
                nueva.setEstadoMID(estadoMID);
                nueva.setNombre(nombre);
                nueva.setDescripcion(descripcion);
                nueva.setFechaInicioEstimada(tsToString(ini));
                nueva.setFechaFinEstimada(tsToString(fin));
                nueva.setRetrasoInicio(null);
                nueva.setRetrasoFin(null);

                misiones.add(nueva);
                escribirEnJsonSinLock(misiones);
                return nuevoId;
            } finally {
                misionLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al registrar mision: " + e.getMessage());
        }
        return -1;
    }

    static void modificar(int usuarioIDLogueado, int id, String nombre, String desc, Timestamp ini, Timestamp fin, AccesoDatos ad) {
        try {
            misionLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
                List<Mision> misiones = mapper.readValue(ruta.toFile(), new TypeReference<List<Mision>>() {});
                StringBuilder descChanges = new StringBuilder();
                for (Mision m : misiones) {
                    if (m.getMisionID() == id) {
                        if (nombre != null && !nombre.equals(m.getNombre())) {
                            if (descChanges.length() > 0) descChanges.append("|");
                            descChanges.append("Nombre:").append(m.getNombre()).append("->").append(nombre);
                            m.setNombre(nombre);
                        }
                        if (desc != null && !desc.equals(m.getDescripcion())) {
                            if (descChanges.length() > 0) descChanges.append("|");
                            descChanges.append("Descripcion:").append(m.getDescripcion()).append("->").append(desc);
                            m.setDescripcion(desc);
                        }
                        String newIni = tsToString(ini);
                        if (newIni != null && !newIni.equals(m.getFechaInicioEstimada())) {
                            if (descChanges.length() > 0) descChanges.append("|");
                            descChanges.append("FechaInicio:").append(m.getFechaInicioEstimada()).append("->").append(newIni);
                            m.setFechaInicioEstimada(newIni);
                        }
                        String newFin = tsToString(fin);
                        if (newFin != null && !newFin.equals(m.getFechaFinEstimada())) {
                            if (descChanges.length() > 0) descChanges.append("|");
                            descChanges.append("FechaFin:").append(m.getFechaFinEstimada()).append("->").append(newFin);
                            m.setFechaFinEstimada(newFin);
                        }
                        break;
                    }
                }
                escribirEnJsonSinLock(misiones);
                if (descChanges.length() > 0) {
                    ad.registrarLog(usuarioIDLogueado, 2, 1, id, descChanges.toString());
                }
            } finally {
                misionLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al modificar mision: " + e.getMessage());
        }
    }

    static void actualizarEstado(int usuarioIDLogueado, int id, int estadoID, Integer retrasoInicio, Integer retrasoFin, AccesoDatos ad) {
        try {
            misionLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
                List<Mision> misiones = mapper.readValue(ruta.toFile(), new TypeReference<List<Mision>>() {});
                int accionID = 0;
                for (Mision m : misiones) {
                    if (m.getMisionID() == id) {
                        String estadoAnterior = obtenerNombreEstadoPorId(m.getEstadoMID());
                        m.setEstadoMID(estadoID);
                        if (retrasoInicio != null) m.setRetrasoInicio(retrasoInicio);
                        if (retrasoFin != null) m.setRetrasoFin(retrasoFin);
                        String estadoNuevo = obtenerNombreEstadoPorId(estadoID);
                        if (estadoID == 5) accionID = 3;
                        else if (estadoID == 4) accionID = 4;
                        else accionID = 2;
                        String desc = "Estado:" + estadoAnterior + "->" + estadoNuevo;
                        escribirEnJsonSinLock(misiones);
                        ad.registrarLog(usuarioIDLogueado, accionID, 1, id, desc);
                        return;
                    }
                }
                escribirEnJsonSinLock(misiones);
            } finally {
                misionLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al actualizar estado de mision: " + e.getMessage());
        }
    }

    static ResultSet listar() throws SQLException {
        List<Mision> misiones = leerDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (Mision m : misiones) {
            filas.add(new String[]{
                String.valueOf(m.getMisionID()),
                m.getNombre() != null ? m.getNombre() : "",
                m.getFechaInicioEstimada() != null ? m.getFechaInicioEstimada() : "",
                m.getFechaFinEstimada() != null ? m.getFechaFinEstimada() : "",
                m.getRetrasoInicio() != null ? String.valueOf(m.getRetrasoInicio()) : "",
                m.getRetrasoFin() != null ? String.valueOf(m.getRetrasoFin()) : "",
                obtenerNombreEstadoPorId(m.getEstadoMID())
            });
        }
        return new AccesoDatos.SimpleResultSet(filas, 7);
    }

    static ResultSet consultar(int id) throws SQLException {
        List<Mision> misiones = leerDesdeJson();
        for (Mision m : misiones) {
            if (m.getMisionID() == id) {
                List<String[]> filas = new ArrayList<>();
                filas.add(new String[]{
                    String.valueOf(m.getMisionID()),
                    m.getNombre() != null ? m.getNombre() : "",
                    m.getDescripcion() != null ? m.getDescripcion() : "",
                    obtenerNombreEstadoPorId(m.getEstadoMID()),
                    m.getFechaInicioEstimada() != null ? m.getFechaInicioEstimada() : "",
                    m.getFechaFinEstimada() != null ? m.getFechaFinEstimada() : "",
                    m.getRetrasoInicio() != null ? String.valueOf(m.getRetrasoInicio()) : "",
                    m.getRetrasoFin() != null ? String.valueOf(m.getRetrasoFin()) : ""
                });
                return new AccesoDatos.SimpleResultSet(filas, 8);
            }
        }
        return new AccesoDatos.SimpleResultSet(new ArrayList<>(), 8);
    }

    static boolean existe(int id) throws SQLException {
        List<Mision> misiones = leerDesdeJson();
        for (Mision m : misiones) {
            if (m.getMisionID() == id) return true;
        }
        return false;
    }

    static boolean estaTerminada(int id) {
        List<Mision> misiones = leerDesdeJson();
        for (Mision m : misiones) {
            if (m.getMisionID() == id) return m.estaTerminada();
        }
        return false;
    }

    // --- Group/Assignment operations ---

    static void asignarTripulante(int usuarioIDLogueado, int tripID, int misID, Timestamp fecha, AccesoDatos ad) {
        try {
            misionLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "GrupoMisiones.json");
                List<GrupoMision> grupos = mapper.readValue(ruta.toFile(), new TypeReference<List<GrupoMision>>() {});
                GrupoMision nuevo = new GrupoMision();
                nuevo.TripulanteID = tripID;
                nuevo.MisionID = misID;
                nuevo.FechaAsignacion = tsToString(fecha);
                grupos.add(nuevo);
                escribirGrupoMisionesEnJsonSinLock(grupos);
                String descLog = "TripulanteID=" + tripID + "|MisionID=" + misID;
                ad.registrarLog(usuarioIDLogueado, 5, 1, misID, descLog);
            } finally {
                misionLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al asignar tripulante a mision: " + e.getMessage());
        }
    }

    static ResultSet listarMisionesTripulante(int tripulanteID) throws SQLException {
        List<GrupoMision> grupos = leerGrupoMisionesDesdeJson();
        List<Mision> misiones = leerDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (GrupoMision g : grupos) {
            if (g.TripulanteID == tripulanteID) {
                for (Mision m : misiones) {
                    if (m.getMisionID() == g.MisionID) {
                        filas.add(new String[]{
                            String.valueOf(m.getMisionID()),
                            m.getNombre() != null ? m.getNombre() : "",
                            obtenerNombreEstadoPorId(m.getEstadoMID()),
                            m.getFechaInicioEstimada() != null ? m.getFechaInicioEstimada() : "",
                            m.getFechaFinEstimada() != null ? m.getFechaFinEstimada() : ""
                        });
                        break;
                    }
                }
            }
        }
        return new AccesoDatos.SimpleResultSet(filas, 5);
    }
}
