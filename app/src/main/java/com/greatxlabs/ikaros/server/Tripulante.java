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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // --- Inner classes ---

    static class EstadoTripulante {
        public int EstadoTID;
        public String Estado;
        public EstadoTripulante() {}
    }

    static class SexoJson {
        public int SexoID;
        public String Sexo;
        public SexoJson() {}
    }

    static class AptitudJson {
        public int AptitudID;
        public String Aptitud;
        public AptitudJson() {}
    }

    // --- Infrastructure ---

    private static final ObjectMapper mapper = new ObjectMapper();
    static final SemaforoRW tripulanteLock = new SemaforoRW();

    // --- JSON I/O ---

    static List<Tripulante> leerDesdeJson() {
        try {
            tripulanteLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<Tripulante>>() {});
            } finally {
                tripulanteLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer Tripulantes.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static void escribirEnJsonSinLock(List<Tripulante> tripulantes) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Tripulantes", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), tripulantes);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    static List<Capacidad> leerCapacidadesDesdeJson() {
        try {
            tripulanteLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<Capacidad>>() {});
            } finally {
                tripulanteLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer Capacidades.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static void escribirCapacidadesEnJsonSinLock(List<Capacidad> capacidades) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Capacidades", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), capacidades);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
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

    static List<EstadoTripulante> leerEstadosDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "EstadosTripulantes.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EstadoTripulante>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer EstadosTripulantes.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static List<SexoJson> leerSexosDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Sexos.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<SexoJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Sexos.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- Lookups ---

    static String obtenerNombreEstadoPorId(int estadoTID) {
        List<EstadoTripulante> estados = leerEstadosDesdeJson();
        for (EstadoTripulante e : estados) {
            if (e.EstadoTID == estadoTID) return e.Estado;
        }
        return null;
    }

    static String obtenerNombreSexoPorId(int sexoID) {
        List<SexoJson> sexos = leerSexosDesdeJson();
        for (SexoJson s : sexos) {
            if (s.SexoID == sexoID) return s.Sexo;
        }
        return null;
    }

    static String obtenerNombreAptitudPorId(int aptitudID) {
        List<AptitudJson> aptitudes = leerAptitudesDesdeJson();
        for (AptitudJson a : aptitudes) {
            if (a.AptitudID == aptitudID) return a.Aptitud;
        }
        return null;
    }

    static String obtenerNombreCompletoPorId(int tripulanteID) {
        List<Tripulante> tripulantes = leerDesdeJson();
        for (Tripulante t : tripulantes) {
            if (t.getTripulanteID() == tripulanteID) {
                String nombre = t.getNombre() != null ? t.getNombre() : "";
                String apellido = t.getApellido() != null ? t.getApellido() : "";
                return (nombre + " " + apellido).trim();
            }
        }
        return null;
    }

    // --- Map helpers ---

    static Map<String, Integer> obtenerEstadosComoMapa() {
        Map<String, Integer> mapa = new HashMap<>();
        List<EstadoTripulante> estados = leerEstadosDesdeJson();
        for (EstadoTripulante e : estados) {
            mapa.put(e.Estado.toUpperCase(), e.EstadoTID);
        }
        return mapa;
    }

    static Map<String, Integer> obtenerAptitudesComoMapa() {
        Map<String, Integer> mapa = new HashMap<>();
        List<AptitudJson> aptitudes = leerAptitudesDesdeJson();
        for (AptitudJson a : aptitudes) {
            mapa.put(a.Aptitud.toUpperCase(), a.AptitudID);
        }
        return mapa;
    }

    // --- CRUD ---

    static ResultSet listarEstados() {
        List<EstadoTripulante> estados = leerEstadosDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (EstadoTripulante e : estados) {
            filas.add(new String[]{String.valueOf(e.EstadoTID), e.Estado});
        }
        return new AccesoDatos.SimpleResultSet(filas, 2);
    }

    static ResultSet registrar(int estadoTID, int sexoID, int peso, int altura,
            String nombre, String apellido, String imagen, Date fechaNacimiento) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                List<Tripulante> tripulantes = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Tripulante>>() {});

                int nuevoId = 1;
                for (Tripulante t : tripulantes) {
                    if (t.getTripulanteID() >= nuevoId) nuevoId = t.getTripulanteID() + 1;
                }

                Tripulante nuevo = new Tripulante();
                nuevo.setTripulanteID(nuevoId);
                nuevo.setEstadoTID(estadoTID);
                nuevo.setSexoID(sexoID);
                nuevo.setPeso(peso);
                nuevo.setAltura(altura);
                nuevo.setNombre(nombre);
                nuevo.setApellido(apellido);
                nuevo.setImagen(imagen);
                nuevo.setFechaDeNacimiento(fechaNacimiento.toString());

                tripulantes.add(nuevo);
                escribirEnJsonSinLock(tripulantes);

                List<String[]> filas = new ArrayList<>();
                filas.add(new String[]{String.valueOf(nuevoId)});
                return new AccesoDatos.SimpleResultSet(filas, 1);
            } finally {
                tripulanteLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            List<String[]> filas = new ArrayList<>();
            filas.add(new String[]{"-1"});
            return new AccesoDatos.SimpleResultSet(filas, 1);
        } catch (IOException e) {
            System.err.println("Error al registrar tripulante: " + e.getMessage());
            return new AccesoDatos.SimpleResultSet(new ArrayList<>(), 1);
        }
    }

    static void modificar(int usuarioIDLogueado, int tripulanteID, int estadoTID, int sexoID, int peso, int altura,
            String nombre, String apellido, String imagen, Date fechaNacimiento, AccesoDatos ad) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                List<Tripulante> tripulantes = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Tripulante>>() {});
                StringBuilder desc = new StringBuilder();

                for (Tripulante t : tripulantes) {
                    if (t.getTripulanteID() == tripulanteID) {
                        if (t.getEstadoTID() != estadoTID) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Estado:").append(obtenerNombreEstadoPorId(t.getEstadoTID())).append("->").append(obtenerNombreEstadoPorId(estadoTID));
                            t.setEstadoTID(estadoTID);
                        }
                        if (t.getSexoID() != sexoID) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Sexo:").append(obtenerNombreSexoPorId(t.getSexoID())).append("->").append(obtenerNombreSexoPorId(sexoID));
                            t.setSexoID(sexoID);
                        }
                        if (t.getPeso() != peso) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Peso:").append(t.getPeso()).append("->").append(peso);
                            t.setPeso(peso);
                        }
                        if (t.getAltura() != altura) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Altura:").append(t.getAltura()).append("->").append(altura);
                            t.setAltura(altura);
                        }
                        if (nombre != null && !nombre.equals(t.getNombre())) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Nombre:").append(t.getNombre()).append("->").append(nombre);
                            t.setNombre(nombre);
                        }
                        if (apellido != null && !apellido.equals(t.getApellido())) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Apellido:").append(t.getApellido()).append("->").append(apellido);
                            t.setApellido(apellido);
                        }
                        if (imagen != null && !imagen.equals(t.getImagen())) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Imagen:").append(t.getImagen()).append("->").append(imagen);
                            t.setImagen(imagen);
                        }
                        String newFecha = fechaNacimiento.toString();
                        if (!newFecha.equals(t.getFechaDeNacimiento())) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("FechaNacimiento:").append(t.getFechaDeNacimiento()).append("->").append(newFecha);
                            t.setFechaDeNacimiento(newFecha);
                        }
                        break;
                    }
                }

                escribirEnJsonSinLock(tripulantes);
                if (desc.length() > 0) {
                    ad.registrarLog(usuarioIDLogueado, 9, 2, tripulanteID, desc.toString());
                }
            } finally {
                tripulanteLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operacion interrumpida al modificar tripulante: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al modificar tripulante: " + e.getMessage());
        }
    }

    static void baja(int usuarioIDLogueado, int tripulanteID, AccesoDatos ad) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                List<Tripulante> tripulantes = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Tripulante>>() {});

                for (Tripulante t : tripulantes) {
                    if (t.getTripulanteID() == tripulanteID) {
                        String estadoAnterior = obtenerNombreEstadoPorId(t.getEstadoTID());
                        t.retirar();
                        String estadoActual = obtenerNombreEstadoPorId(t.getEstadoTID());
                        escribirEnJsonSinLock(tripulantes);
                        String desc = "Estado:" + estadoAnterior + "->" + estadoActual;
                        ad.registrarLog(usuarioIDLogueado, 10, 2, tripulanteID, desc);
                        return;
                    }
                }

                escribirEnJsonSinLock(tripulantes);
            } finally {
                tripulanteLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operacion interrumpida al dar de baja tripulante: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al dar de baja tripulante: " + e.getMessage());
        }
    }

    static ResultSet listar() {
        List<Tripulante> tripulantes = leerDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (Tripulante t : tripulantes) {
            filas.add(new String[]{
                String.valueOf(t.getTripulanteID()),
                t.getNombre() != null ? t.getNombre() : "",
                t.getApellido() != null ? t.getApellido() : "",
                t.getImagen() != null ? t.getImagen() : "",
                obtenerNombreEstadoPorId(t.getEstadoTID()),
                obtenerNombreSexoPorId(t.getSexoID()),
                String.valueOf(t.getPeso()),
                String.valueOf(t.getAltura())
            });
        }
        return new AccesoDatos.SimpleResultSet(filas, 8);
    }

    static ResultSet listarMision(int misionID) {
        List<Mision.GrupoMision> grupos = Mision.leerGrupoMisionesDesdeJson();
        List<Tripulante> tripulantes = leerDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (Mision.GrupoMision g : grupos) {
            if (g.MisionID == misionID) {
                for (Tripulante t : tripulantes) {
                    if (t.getTripulanteID() == g.TripulanteID) {
                        filas.add(new String[]{
                            String.valueOf(t.getTripulanteID()),
                            t.getNombre() != null ? t.getNombre() : "",
                            t.getApellido() != null ? t.getApellido() : "",
                            obtenerNombreEstadoPorId(t.getEstadoTID()),
                            g.FechaAsignacion != null ? g.FechaAsignacion : ""
                        });
                        break;
                    }
                }
            }
        }
        return new AccesoDatos.SimpleResultSet(filas, 5);
    }

    static ResultSet consultar(int tripulanteID) {
        List<Tripulante> tripulantes = leerDesdeJson();
        for (Tripulante t : tripulantes) {
            if (t.getTripulanteID() == tripulanteID) {
                List<String[]> filas = new ArrayList<>();
                filas.add(new String[]{
                    String.valueOf(t.getTripulanteID()),
                    t.getNombre() != null ? t.getNombre() : "",
                    t.getApellido() != null ? t.getApellido() : "",
                    String.valueOf(t.getPeso()),
                    String.valueOf(t.getAltura()),
                    t.getImagen() != null ? t.getImagen() : "",
                    t.getFechaDeNacimiento() != null ? t.getFechaDeNacimiento() : "",
                    obtenerNombreEstadoPorId(t.getEstadoTID()),
                    obtenerNombreSexoPorId(t.getSexoID())
                });
                return new AccesoDatos.SimpleResultSet(filas, 9);
            }
        }
        return new AccesoDatos.SimpleResultSet(new ArrayList<>(), 9);
    }

    static boolean existe(int id) {
        List<Tripulante> tripulantes = leerDesdeJson();
        for (Tripulante t : tripulantes) {
            if (t.getTripulanteID() == id) return true;
        }
        return false;
    }

    static boolean estaRetirado(int id) {
        List<Tripulante> tripulantes = leerDesdeJson();
        for (Tripulante t : tripulantes) {
            if (t.getTripulanteID() == id) return t.estaRetirado();
        }
        return false;
    }

    // --- Capacidad operations ---

    static ResultSet consultarCapacidades(int tripulanteID) {
        List<Capacidad> capacidades = leerCapacidadesDesdeJson();
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

    static void registrarCapacidad(int usuarioIDLogueado, int tripulanteID, int aptitudID, int calificacion, String fecha, AccesoDatos ad) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                List<Capacidad> capacidades = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Capacidad>>() {});

                for (Capacidad c : capacidades) {
                    if (c.getTripulanteID() == tripulanteID && c.getAptitudID() == aptitudID) {
                        c.setCalificacion(calificacion);
                        c.setFechaCapacidades(fecha);
                        escribirCapacidadesEnJsonSinLock(capacidades);
                        return;
                    }
                }

                Capacidad nueva = new Capacidad();
                nueva.setTripulanteID(tripulanteID);
                nueva.setAptitudID(aptitudID);
                nueva.setCalificacion(calificacion);
                nueva.setFechaCapacidades(fecha);
                capacidades.add(nueva);
                escribirCapacidadesEnJsonSinLock(capacidades);

                String nombreAptitud = obtenerNombreAptitudPorId(aptitudID);
                String nombreTripulante = obtenerNombreCompletoPorId(tripulanteID);
                String desc = "Aptitud=" + (nombreAptitud != null ? nombreAptitud : aptitudID)
                        + "|Tripulante=" + (nombreTripulante != null ? nombreTripulante : tripulanteID)
                        + "|Calificacion=" + calificacion
                        + "|Fecha=" + (fecha != null ? fecha : "");
                ad.registrarLog(usuarioIDLogueado, 11, 2, tripulanteID, desc);
            } finally {
                tripulanteLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operacion interrumpida al registrar capacidad: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al registrar capacidad: " + e.getMessage());
        }
    }

    static void eliminarCapacidades(int tripulanteID) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                List<Capacidad> capacidades = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Capacidad>>() {});

                capacidades.removeIf(c -> c.getTripulanteID() == tripulanteID);
                escribirCapacidadesEnJsonSinLock(capacidades);
            } finally {
                tripulanteLock.terminarEscritura();
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
