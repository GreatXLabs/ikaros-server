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
        Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING);
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
        String desc = null;
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                List<Tripulante> tripulantes = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Tripulante>>() {});
                StringBuilder descBuilder = new StringBuilder();

                for (Tripulante t : tripulantes) {
                    if (t.getTripulanteID() == tripulanteID) {
                        if (t.getEstadoTID() != estadoTID) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Estado:").append(obtenerNombreEstadoPorId(t.getEstadoTID())).append("->").append(obtenerNombreEstadoPorId(estadoTID));
                            t.setEstadoTID(estadoTID);
                        }
                        if (t.getSexoID() != sexoID) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Sexo:").append(obtenerNombreSexoPorId(t.getSexoID())).append("->").append(obtenerNombreSexoPorId(sexoID));
                            t.setSexoID(sexoID);
                        }
                        if (t.getPeso() != peso) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Peso:").append(t.getPeso()).append("->").append(peso);
                            t.setPeso(peso);
                        }
                        if (t.getAltura() != altura) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Altura:").append(t.getAltura()).append("->").append(altura);
                            t.setAltura(altura);
                        }
                        if (nombre != null && !nombre.equals(t.getNombre())) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Nombre:").append(t.getNombre()).append("->").append(nombre);
                            t.setNombre(nombre);
                        }
                        if (apellido != null && !apellido.equals(t.getApellido())) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Apellido:").append(t.getApellido()).append("->").append(apellido);
                            t.setApellido(apellido);
                        }
                        if (imagen != null && !imagen.equals(t.getImagen())) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Imagen:").append(t.getImagen()).append("->").append(imagen);
                            t.setImagen(imagen);
                        }
                        String newFecha = fechaNacimiento.toString();
                        if (!newFecha.equals(t.getFechaDeNacimiento())) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("FechaNacimiento:").append(t.getFechaDeNacimiento()).append("->").append(newFecha);
                            t.setFechaDeNacimiento(newFecha);
                        }
                        break;
                    }
                }

                escribirEnJsonSinLock(tripulantes);
                if (descBuilder.length() > 0) {
                    desc = descBuilder.toString();
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
        if (desc != null) {
            ad.registrarLog(usuarioIDLogueado, 9, 2, tripulanteID, desc);
        }
    }

    static void baja(int usuarioIDLogueado, int tripulanteID, AccesoDatos ad) {
        String desc = null;
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
                        desc = "Estado:" + estadoAnterior + "->" + estadoActual;
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
        if (desc != null) {
            ad.registrarLog(usuarioIDLogueado, 10, 2, tripulanteID, desc);
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

}
