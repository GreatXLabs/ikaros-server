package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AccesoDatos {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final SemaforoRW jsonLock = new SemaforoRW();
    private static final SemaforoRW tripulanteLock = new SemaforoRW();
    private static final SemaforoRW misionLock = new SemaforoRW();
    private static final SemaforoRW eventoLock = new SemaforoRW();
    private static final SemaforoRW registroLock = new SemaforoRW();

    private static void asegurarArchivo(String nombre) throws IOException {
        Path destino = Path.of(Configuracion.getDataDir(), nombre);
        if (Files.exists(destino)) return;
        Files.createDirectories(destino.getParent());
        try (InputStream is = AccesoDatos.class.getClassLoader().getResourceAsStream(nombre)) {
            if (is == null) throw new IOException("Recurso semilla no encontrado: " + nombre);
            Files.copy(is, destino);
        }
        System.out.println("Archivo sembrado: " + destino);
    }

    static {
        try {
            asegurarArchivo("Usuarios.json");
            asegurarArchivo("Roles.json");
            asegurarArchivo("EstadosUsuarios.json");
            asegurarArchivo("Tripulantes.json");
            asegurarArchivo("Capacidades.json");
            asegurarArchivo("Aptitudes.json");
            asegurarArchivo("EstadosTripulantes.json");
            asegurarArchivo("Sexos.json");
            asegurarArchivo("Misiones.json");
            asegurarArchivo("EstadosMisiones.json");
            asegurarArchivo("GrupoMisiones.json");
            asegurarArchivo("Eventos.json");
            asegurarArchivo("EstadosEventos.json");
            asegurarArchivo("Acciones.json");
            asegurarArchivo("Entidades.json");
            asegurarArchivo("Registros.json");
        } catch (IOException e) {
            System.err.println("Error inicializando archivos de datos: " + e.getMessage());
        }
    }

    private static class UsuarioJson {
        public int UsuarioID;
        public int RolID;
        public int EstadoUID;
        public String Nombre;
        public String Apellido;
        public String Usuario;
        public String Clave;
        public UsuarioJson() {}
    }

    private static class RolJson {
        public int RolID;
        public String Rol;
        public RolJson() {}
    }

    public static class UsuarioLoginResult {
        public int usuarioID;
        public String rol;
        public UsuarioLoginResult() {}
    }

    private static class EstadoJson {
        public int EstadoUID;
        public String Estado;
        public EstadoJson() {}
    }

    private static class TripulanteJson {
        public int TripulanteID;
        public int EstadoTID;
        public int SexoID;
        public int Peso;
        public int Altura;
        public String Nombre;
        public String Apellido;
        public String Imagen;
        public String FechaDeNacimiento;
        public TripulanteJson() {}
    }

    private static class CapacidadJson {
        public int TripulanteID;
        public int AptitudID;
        public int Calificacion;
        public String FechaCapacidades;
        public CapacidadJson() {}
    }

    public static class AptitudJson {
        public int AptitudID;
        public String Aptitud;
        public AptitudJson() {}
    }

    private static class EstadoTripulanteJson {
        public int EstadoTID;
        public String Estado;
        public EstadoTripulanteJson() {}
    }

    private static class SexoJson {
        public int SexoID;
        public String Sexo;
        public SexoJson() {}
    }

    private static class MisionJson {
        public int MisionID;
        public int EstadoMID;
        public Integer RetrasoInicio;
        public String FechaInicioEstimada;
        public String FechaFinEstimada;
        public Integer RetrasoFin;
        public String Nombre;
        public String Descripcion;
        public MisionJson() {}
    }

    private static class EstadoMisionJson {
        public int EstadoMID;
        public String Estado;
        public EstadoMisionJson() {}
    }

    private static class GrupoMisionJson {
        public int TripulanteID;
        public int MisionID;
        public String FechaAsignacion;
        public GrupoMisionJson() {}
    }

    private static class EventoJson {
        public int EventoID;
        public int MisionID;
        public String Titulo;
        public String Fecha;
        public String Descripcion;
        public int EstadoEID;
        public EventoJson() {}
    }

    private static class EstadoEventoJson {
        public int EstadoEID;
        public String Estado;
        public EstadoEventoJson() {}
    }

    private static class AccionJson {
        public int AccionID;
        public String Accion;
        public AccionJson() {}
    }

    private static class EntidadJson {
        public int TipoEntidadID;
        public String TipoEntidad;
        public EntidadJson() {}
    }

    private static class RegistroJson {
        public int RegistroID;
        public int AccionMID;
        public int UsuarioID;
        public int TipoEntidadID;
        public int EntidadID;
        public String FechaHora;
        public String Descripcion;
        public RegistroJson() {}
    }

    private List<UsuarioJson> leerUsuariosDesdeJson() {
        try {
            jsonLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<UsuarioJson>>() {});
            } finally {
                jsonLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer Usuarios.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void escribirUsuariosEnJsonSinLock(List<UsuarioJson> usuarios) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
        Path tmp  = Files.createTempFile(ruta.getParent(), "Usuarios", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), usuarios);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private List<RolJson> leerRolesDesdeJson() {
        try {
            jsonLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Roles.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<RolJson>>() {});
            } finally {
                jsonLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer Roles.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EstadoJson> leerEstadosDesdeJson() {
        try {
            jsonLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "EstadosUsuarios.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<EstadoJson>>() {});
            } finally {
                jsonLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer EstadosUsuarios.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<TripulanteJson> leerTripulantesDesdeJson() {
        try {
            tripulanteLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<TripulanteJson>>() {});
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

    private void escribirTripulantesEnJsonSinLock(List<TripulanteJson> tripulantes) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
        Path tmp  = Files.createTempFile(ruta.getParent(), "Tripulantes", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), tripulantes);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private List<CapacidadJson> leerCapacidadesDesdeJson() {
        try {
            tripulanteLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<CapacidadJson>>() {});
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

    private void escribirCapacidadesEnJsonSinLock(List<CapacidadJson> capacidades) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
        Path tmp  = Files.createTempFile(ruta.getParent(), "Capacidades", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), capacidades);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private List<AptitudJson> leerAptitudesDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Aptitudes.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<AptitudJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Aptitudes.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EstadoTripulanteJson> leerEstadosTripulantesDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "EstadosTripulantes.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EstadoTripulanteJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer EstadosTripulantes.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<MisionJson> leerMisionesDesdeJson() {
        try {
            misionLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<MisionJson>>() {});
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

    private void escribirMisionesEnJsonSinLock(List<MisionJson> misiones) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Misiones", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), misiones);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private List<EstadoMisionJson> leerEstadosMisionDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "EstadosMisiones.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EstadoMisionJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer EstadosMisiones.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<GrupoMisionJson> leerGrupoMisionesDesdeJson() {
        try {
            misionLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "GrupoMisiones.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<GrupoMisionJson>>() {});
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

    private void escribirGrupoMisionesEnJsonSinLock(List<GrupoMisionJson> grupos) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "GrupoMisiones.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "GrupoMisiones", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), grupos);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private List<EventoJson> leerEventosDesdeJson() {
        try {
            eventoLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Eventos.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<EventoJson>>() {});
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

    private void escribirEventosEnJsonSinLock(List<EventoJson> eventos) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Eventos.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Eventos", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), eventos);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private List<EstadoEventoJson> leerEstadosEventoDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "EstadosEventos.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EstadoEventoJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer EstadosEventos.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<AccionJson> leerAccionesDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Acciones.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<AccionJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Acciones.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EntidadJson> leerEntidadesDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Entidades.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EntidadJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Entidades.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<RegistroJson> leerRegistrosDesdeJson() {
        try {
            registroLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Registros.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<RegistroJson>>() {});
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

    private void escribirRegistrosEnJsonSinLock(List<RegistroJson> registros) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Registros.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Registros", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), registros);
        Files.move(tmp, ruta,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private String obtenerNombreAccionPorId(int accionID) {
        List<AccionJson> acciones = leerAccionesDesdeJson();
        for (AccionJson a : acciones) {
            if (a.AccionID == accionID) return a.Accion;
        }
        return null;
    }

    private String obtenerNombreEntidadPorId(int tipoEntidadID) {
        List<EntidadJson> entidades = leerEntidadesDesdeJson();
        for (EntidadJson e : entidades) {
            if (e.TipoEntidadID == tipoEntidadID) return e.TipoEntidad;
        }
        return null;
    }

    private List<SexoJson> leerSexosDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Sexos.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<SexoJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Sexos.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private String obtenerNombreEstadoTripulantePorId(int estadoTID) {
        List<EstadoTripulanteJson> estados = leerEstadosTripulantesDesdeJson();
        for (EstadoTripulanteJson e : estados) {
            if (e.EstadoTID == estadoTID) return e.Estado;
        }
        return null;
    }

    private String obtenerNombreSexoPorId(int sexoID) {
        List<SexoJson> sexos = leerSexosDesdeJson();
        for (SexoJson s : sexos) {
            if (s.SexoID == sexoID) return s.Sexo;
        }
        return null;
    }

    private String obtenerNombreAptitudPorId(int aptitudID) {
        List<AptitudJson> aptitudes = leerAptitudesDesdeJson();
        for (AptitudJson a : aptitudes) {
            if (a.AptitudID == aptitudID) return a.Aptitud;
        }
        return null;
    }

    private String obtenerNombreRolPorId(int rolId) {
        List<RolJson> roles = leerRolesDesdeJson();
        for (RolJson r : roles) {
            if (r.RolID == rolId) {
                return r.Rol;
            }
        }
        return null;
    }

    private String obtenerNombreEstadoPorId(int estadoUid) {
        List<EstadoJson> estados = leerEstadosDesdeJson();
        for (EstadoJson e : estados) {
            if (e.EstadoUID == estadoUid) {
                return e.Estado;
            }
        }
        return null;
    }

    private String obtenerNombreEstadoMisionPorId(int estadoMID) {
        List<EstadoMisionJson> estados = leerEstadosMisionDesdeJson();
        for (EstadoMisionJson e : estados) {
            if (e.EstadoMID == estadoMID) return e.Estado;
        }
        return null;
    }

    private String obtenerNombreEstadoEventoPorId(int estadoEID) {
        List<EstadoEventoJson> estados = leerEstadosEventoDesdeJson();
        for (EstadoEventoJson e : estados) {
            if (e.EstadoEID == estadoEID) return e.Estado;
        }
        return null;
    }

    private String obtenerNombreMisionPorId(int misionID) {
        List<MisionJson> misiones = leerMisionesDesdeJson();
        for (MisionJson m : misiones) {
            if (m.MisionID == misionID) return m.Nombre;
        }
        return null;
    }

    private static String tsToString(Timestamp ts) {
        if (ts == null) return null;
        return ts.toLocalDateTime().toString();
    }

    private static final String[] COLUMNAS_USUARIO = {
        "ID", "USUARIO", "NOMBRE", "APELLIDO", "CLAVE", "ROLNOMBRE", "ROLID", "ESTADONOMBRE"
    };

    private class UsuarioResultSet implements ResultSet {

        private final List<Map<String, Object>> filas;
        private int indiceActual = -1;

        UsuarioResultSet(List<Map<String, Object>> filas) {
            this.filas = filas;
        }

        @Override
        public boolean next() throws SQLException {
            indiceActual++;
            return indiceActual < filas.size();
        }

        @Override
        public void close() throws SQLException {}

        @Override
        public boolean wasNull() throws SQLException { return false; }

        private Map<String, Object> filaActual() throws SQLException {
            if (indiceActual < 0 || indiceActual >= filas.size()) {
                throw new SQLException("No hay fila actual");
            }
            return filas.get(indiceActual);
        }

        @Override
        public Object getObject(String columnLabel) throws SQLException {
            return filaActual().get(columnLabel.toUpperCase());
        }

        @Override
        public String getString(String columnLabel) throws SQLException {
            Object v = getObject(columnLabel);
            return v == null ? null : v.toString();
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            Object v = getObject(columnLabel);
            return v == null ? 0 : Integer.parseInt(v.toString());
        }

        @Override
        public Timestamp getTimestamp(String columnLabel) throws SQLException {
            Object v = getObject(columnLabel);
            return v == null ? null : Timestamp.valueOf(v.toString());
        }

        @Override
        public String getString(int i) throws SQLException {
            if (i < 1 || i > COLUMNAS_USUARIO.length) return null;
            Object v = filaActual().get(COLUMNAS_USUARIO[i - 1]);
            return v == null ? null : v.toString();
        }
        @Override public boolean getBoolean(int i) throws SQLException { return false; }
        @Override public byte getByte(int i) throws SQLException { return 0; }
        @Override public short getShort(int i) throws SQLException { return 0; }
        @Override public int getInt(int i) throws SQLException { return 0; }
        @Override public long getLong(int i) throws SQLException { return 0; }
        @Override public float getFloat(int i) throws SQLException { return 0; }
        @Override public double getDouble(int i) throws SQLException { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(int i, int scale) throws SQLException { return null; }
        @Override public byte[] getBytes(int i) throws SQLException { return null; }
        @Override public java.sql.Date getDate(int i) throws SQLException { return null; }
        @Override public java.sql.Time getTime(int i) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(int i) throws SQLException { return null; }
        @Override public InputStream getAsciiStream(int i) throws SQLException { return null; }
        @Override public InputStream getUnicodeStream(int i) throws SQLException { return null; }
        @Override public InputStream getBinaryStream(int i) throws SQLException { return null; }

        @Override public boolean getBoolean(String c) throws SQLException { return false; }
        @Override public byte getByte(String c) throws SQLException { return 0; }
        @Override public short getShort(String c) throws SQLException { return 0; }
        @Override public long getLong(String c) throws SQLException { return 0; }
        @Override public float getFloat(String c) throws SQLException { return 0; }
        @Override public double getDouble(String c) throws SQLException { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(String c, int scale) throws SQLException { return null; }
        @Override public java.math.BigDecimal getBigDecimal(String c) throws SQLException { return null; }
        @Override public byte[] getBytes(String c) throws SQLException { return null; }
        @Override public java.sql.Date getDate(String c) throws SQLException { return null; }
        @Override public java.sql.Time getTime(String c) throws SQLException { return null; }
        @Override public InputStream getAsciiStream(String c) throws SQLException { return null; }
        @Override public InputStream getUnicodeStream(String c) throws SQLException { return null; }
        @Override public InputStream getBinaryStream(String c) throws SQLException { return null; }

        @Override public SQLWarning getWarnings() throws SQLException { return null; }
        @Override public void clearWarnings() throws SQLException {}
        @Override public String getCursorName() throws SQLException { return null; }
        @Override public ResultSetMetaData getMetaData() throws SQLException { return null; }
        @Override public Object getObject(int i) throws SQLException { return null; }
        @Override public int findColumn(String c) throws SQLException { return 0; }
        @Override public Reader getCharacterStream(int i) throws SQLException { return null; }
        @Override public Reader getCharacterStream(String c) throws SQLException { return null; }
        @Override public java.math.BigDecimal getBigDecimal(int i) throws SQLException { return null; }
        @Override public boolean isBeforeFirst() throws SQLException { return false; }
        @Override public boolean isAfterLast() throws SQLException { return false; }
        @Override public boolean isFirst() throws SQLException { return false; }
        @Override public boolean isLast() throws SQLException { return false; }
        @Override public void beforeFirst() throws SQLException {}
        @Override public void afterLast() throws SQLException {}
        @Override public boolean first() throws SQLException { return false; }
        @Override public boolean last() throws SQLException { return false; }
        @Override public int getRow() throws SQLException { return indiceActual + 1; }
        @Override public boolean absolute(int row) throws SQLException { return false; }
        @Override public boolean relative(int rows) throws SQLException { return false; }
        @Override public boolean previous() throws SQLException { return false; }
        @Override public void setFetchDirection(int d) throws SQLException {}
        @Override public int getFetchDirection() throws SQLException { return FETCH_FORWARD; }
        @Override public void setFetchSize(int rows) throws SQLException {}
        @Override public int getFetchSize() throws SQLException { return 0; }
        @Override public int getType() throws SQLException { return TYPE_FORWARD_ONLY; }
        @Override public int getConcurrency() throws SQLException { return CONCUR_READ_ONLY; }
        @Override public boolean rowUpdated() throws SQLException { return false; }
        @Override public boolean rowInserted() throws SQLException { return false; }
        @Override public boolean rowDeleted() throws SQLException { return false; }
        @Override public void updateNull(int i) throws SQLException {}
        @Override public void updateBoolean(int i, boolean x) throws SQLException {}
        @Override public void updateByte(int i, byte x) throws SQLException {}
        @Override public void updateShort(int i, short x) throws SQLException {}
        @Override public void updateInt(int i, int x) throws SQLException {}
        @Override public void updateLong(int i, long x) throws SQLException {}
        @Override public void updateFloat(int i, float x) throws SQLException {}
        @Override public void updateDouble(int i, double x) throws SQLException {}
        @Override public void updateBigDecimal(int i, java.math.BigDecimal x) throws SQLException {}
        @Override public void updateString(int i, String x) throws SQLException {}
        @Override public void updateBytes(int i, byte[] x) throws SQLException {}
        @Override public void updateDate(int i, java.sql.Date x) throws SQLException {}
        @Override public void updateTime(int i, java.sql.Time x) throws SQLException {}
        @Override public void updateTimestamp(int i, Timestamp x) throws SQLException {}
        @Override public void updateAsciiStream(int i, InputStream x, int l) throws SQLException {}
        @Override public void updateBinaryStream(int i, InputStream x, int l) throws SQLException {}
        @Override public void updateCharacterStream(int i, Reader x, int l) throws SQLException {}
        @Override public void updateObject(int i, Object x, int s) throws SQLException {}
        @Override public void updateObject(int i, Object x) throws SQLException {}
        @Override public void updateNull(String c) throws SQLException {}
        @Override public void updateBoolean(String c, boolean x) throws SQLException {}
        @Override public void updateByte(String c, byte x) throws SQLException {}
        @Override public void updateShort(String c, short x) throws SQLException {}
        @Override public void updateInt(String c, int x) throws SQLException {}
        @Override public void updateLong(String c, long x) throws SQLException {}
        @Override public void updateFloat(String c, float x) throws SQLException {}
        @Override public void updateDouble(String c, double x) throws SQLException {}
        @Override public void updateBigDecimal(String c, java.math.BigDecimal x) throws SQLException {}
        @Override public void updateString(String c, String x) throws SQLException {}
        @Override public void updateBytes(String c, byte[] x) throws SQLException {}
        @Override public void updateDate(String c, java.sql.Date x) throws SQLException {}
        @Override public void updateTime(String c, java.sql.Time x) throws SQLException {}
        @Override public void updateTimestamp(String c, Timestamp x) throws SQLException {}
        @Override public void updateAsciiStream(String c, InputStream x, int l) throws SQLException {}
        @Override public void updateBinaryStream(String c, InputStream x, int l) throws SQLException {}
        @Override public void updateCharacterStream(String c, Reader x, int l) throws SQLException {}
        @Override public void updateObject(String c, Object x, int s) throws SQLException {}
        @Override public void updateObject(String c, Object x) throws SQLException {}
        @Override public void insertRow() throws SQLException {}
        @Override public void updateRow() throws SQLException {}
        @Override public void deleteRow() throws SQLException {}
        @Override public void refreshRow() throws SQLException {}
        @Override public void cancelRowUpdates() throws SQLException {}
        @Override public void moveToInsertRow() throws SQLException {}
        @Override public void moveToCurrentRow() throws SQLException {}
        @Override public Statement getStatement() throws SQLException { return null; }
        @Override public Object getObject(int i, Map<String, Class<?>> m) throws SQLException { return null; }
        @Override public Ref getRef(int i) throws SQLException { return null; }
        @Override public Blob getBlob(int i) throws SQLException { return null; }
        @Override public Clob getClob(int i) throws SQLException { return null; }
        @Override public Array getArray(int i) throws SQLException { return null; }
        @Override public Object getObject(String c, Map<String, Class<?>> m) throws SQLException { return null; }
        @Override public Ref getRef(String c) throws SQLException { return null; }
        @Override public Blob getBlob(String c) throws SQLException { return null; }
        @Override public Clob getClob(String c) throws SQLException { return null; }
        @Override public Array getArray(String c) throws SQLException { return null; }
        @Override public java.sql.Date getDate(int i, java.util.Calendar cal) throws SQLException { return null; }
        @Override public java.sql.Date getDate(String c, java.util.Calendar cal) throws SQLException { return null; }
        @Override public java.sql.Time getTime(int i, java.util.Calendar cal) throws SQLException { return null; }
        @Override public java.sql.Time getTime(String c, java.util.Calendar cal) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(int i, java.util.Calendar cal) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(String c, java.util.Calendar cal) throws SQLException { return null; }
        @Override public java.net.URL getURL(int i) throws SQLException { return null; }
        @Override public java.net.URL getURL(String c) throws SQLException { return null; }
        @Override public void updateRef(int i, Ref x) throws SQLException {}
        @Override public void updateRef(String c, Ref x) throws SQLException {}
        @Override public void updateBlob(int i, Blob x) throws SQLException {}
        @Override public void updateBlob(String c, Blob x) throws SQLException {}
        @Override public void updateClob(int i, Clob x) throws SQLException {}
        @Override public void updateClob(String c, Clob x) throws SQLException {}
        @Override public void updateArray(int i, Array x) throws SQLException {}
        @Override public void updateArray(String c, Array x) throws SQLException {}
        @Override public RowId getRowId(int i) throws SQLException { return null; }
        @Override public RowId getRowId(String c) throws SQLException { return null; }
        @Override public void updateRowId(int i, RowId x) throws SQLException {}
        @Override public void updateRowId(String c, RowId x) throws SQLException {}
        @Override public int getHoldability() throws SQLException { return HOLD_CURSORS_OVER_COMMIT; }
        @Override public boolean isClosed() throws SQLException { return false; }
        @Override public void updateNString(int i, String x) throws SQLException {}
        @Override public void updateNString(String c, String x) throws SQLException {}
        @Override public void updateNClob(int i, NClob x) throws SQLException {}
        @Override public void updateNClob(String c, NClob x) throws SQLException {}
        @Override public NClob getNClob(int i) throws SQLException { return null; }
        @Override public NClob getNClob(String c) throws SQLException { return null; }
        @Override public SQLXML getSQLXML(int i) throws SQLException { return null; }
        @Override public SQLXML getSQLXML(String c) throws SQLException { return null; }
        @Override public void updateSQLXML(int i, SQLXML x) throws SQLException {}
        @Override public void updateSQLXML(String c, SQLXML x) throws SQLException {}
        @Override public String getNString(int i) throws SQLException { return null; }
        @Override public String getNString(String c) throws SQLException { return null; }
        @Override public Reader getNCharacterStream(int i) throws SQLException { return null; }
        @Override public Reader getNCharacterStream(String c) throws SQLException { return null; }
        @Override public void updateNCharacterStream(int i, Reader x, long l) throws SQLException {}
        @Override public void updateNCharacterStream(String c, Reader x, long l) throws SQLException {}
        @Override public void updateAsciiStream(int i, InputStream x, long l) throws SQLException {}
        @Override public void updateBinaryStream(int i, InputStream x, long l) throws SQLException {}
        @Override public void updateCharacterStream(int i, Reader x, long l) throws SQLException {}
        @Override public void updateAsciiStream(String c, InputStream x, long l) throws SQLException {}
        @Override public void updateBinaryStream(String c, InputStream x, long l) throws SQLException {}
        @Override public void updateCharacterStream(String c, Reader x, long l) throws SQLException {}
        @Override public void updateBlob(int i, InputStream x, long l) throws SQLException {}
        @Override public void updateBlob(String c, InputStream x, long l) throws SQLException {}
        @Override public void updateClob(int i, Reader x, long l) throws SQLException {}
        @Override public void updateClob(String c, Reader x, long l) throws SQLException {}
        @Override public void updateNClob(int i, Reader x, long l) throws SQLException {}
        @Override public void updateNClob(String c, Reader x, long l) throws SQLException {}
        @Override public void updateNCharacterStream(int i, Reader x) throws SQLException {}
        @Override public void updateNCharacterStream(String c, Reader x) throws SQLException {}
        @Override public void updateAsciiStream(int i, InputStream x) throws SQLException {}
        @Override public void updateBinaryStream(int i, InputStream x) throws SQLException {}
        @Override public void updateCharacterStream(int i, Reader x) throws SQLException {}
        @Override public void updateAsciiStream(String c, InputStream x) throws SQLException {}
        @Override public void updateBinaryStream(String c, InputStream x) throws SQLException {}
        @Override public void updateCharacterStream(String c, Reader x) throws SQLException {}
        @Override public void updateBlob(int i, InputStream x) throws SQLException {}
        @Override public void updateBlob(String c, InputStream x) throws SQLException {}
        @Override public void updateClob(int i, Reader x) throws SQLException {}
        @Override public void updateClob(String c, Reader x) throws SQLException {}
        @Override public void updateNClob(int i, Reader x) throws SQLException {}
        @Override public void updateNClob(String c, Reader x) throws SQLException {}
        @Override public <T> T getObject(int i, Class<T> t) throws SQLException { return null; }
        @Override public <T> T getObject(String c, Class<T> t) throws SQLException { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
    }

    private static class SimpleResultSet implements ResultSet {

        private final List<String[]> filas;
        private final int columnas;
        private int indiceActual = -1;

        SimpleResultSet(List<String[]> filas, int columnas) {
            this.filas = filas;
            this.columnas = columnas;
        }

        @Override
        public boolean next() throws SQLException {
            indiceActual++;
            return indiceActual < filas.size();
        }

        @Override
        public void close() throws SQLException {}

        @Override
        public boolean wasNull() throws SQLException { return false; }

        @Override
        public String getString(int i) throws SQLException {
            if (i < 1 || i > columnas || indiceActual < 0 || indiceActual >= filas.size()) return null;
            return filas.get(indiceActual)[i - 1];
        }

        @Override
        public int getInt(int i) throws SQLException {
            String v = getString(i);
            return v == null ? 0 : Integer.parseInt(v);
        }

        @Override
        public boolean getBoolean(int i) throws SQLException { return false; }
        @Override public byte getByte(int i) throws SQLException { return 0; }
        @Override public short getShort(int i) throws SQLException { return 0; }
        @Override public long getLong(int i) throws SQLException { return 0; }
        @Override public float getFloat(int i) throws SQLException { return 0; }
        @Override public double getDouble(int i) throws SQLException { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(int i, int scale) throws SQLException { return null; }
        @Override public java.math.BigDecimal getBigDecimal(int i) throws SQLException { return null; }
        @Override public byte[] getBytes(int i) throws SQLException { return null; }
        @Override public java.sql.Date getDate(int i) throws SQLException { return null; }
        @Override public java.sql.Time getTime(int i) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(int i) throws SQLException { return null; }
        @Override public InputStream getAsciiStream(int i) throws SQLException { return null; }
        @Override public InputStream getUnicodeStream(int i) throws SQLException { return null; }
        @Override public InputStream getBinaryStream(int i) throws SQLException { return null; }

        @Override public String getString(String c) throws SQLException { return null; }
        @Override public boolean getBoolean(String c) throws SQLException { return false; }
        @Override public byte getByte(String c) throws SQLException { return 0; }
        @Override public short getShort(String c) throws SQLException { return 0; }
        @Override public int getInt(String c) throws SQLException { return 0; }
        @Override public long getLong(String c) throws SQLException { return 0; }
        @Override public float getFloat(String c) throws SQLException { return 0; }
        @Override public double getDouble(String c) throws SQLException { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(String c, int scale) throws SQLException { return null; }
        @Override public java.math.BigDecimal getBigDecimal(String c) throws SQLException { return null; }
        @Override public byte[] getBytes(String c) throws SQLException { return null; }
        @Override public java.sql.Date getDate(String c) throws SQLException { return null; }
        @Override public java.sql.Time getTime(String c) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(String c) throws SQLException { return null; }
        @Override public InputStream getAsciiStream(String c) throws SQLException { return null; }
        @Override public InputStream getUnicodeStream(String c) throws SQLException { return null; }
        @Override public InputStream getBinaryStream(String c) throws SQLException { return null; }

        @Override public SQLWarning getWarnings() throws SQLException { return null; }
        @Override public void clearWarnings() throws SQLException {}
        @Override public String getCursorName() throws SQLException { return null; }
        @Override public ResultSetMetaData getMetaData() throws SQLException { return null; }
        @Override public Object getObject(int i) throws SQLException { return getString(i); }
        @Override public Object getObject(String c) throws SQLException { return null; }
        @Override public int findColumn(String c) throws SQLException { return 0; }
        @Override public Reader getCharacterStream(int i) throws SQLException { return null; }
        @Override public Reader getCharacterStream(String c) throws SQLException { return null; }
        @Override public boolean isBeforeFirst() throws SQLException { return false; }
        @Override public boolean isAfterLast() throws SQLException { return false; }
        @Override public boolean isFirst() throws SQLException { return false; }
        @Override public boolean isLast() throws SQLException { return false; }
        @Override public void beforeFirst() throws SQLException {}
        @Override public void afterLast() throws SQLException {}
        @Override public boolean first() throws SQLException { return false; }
        @Override public boolean last() throws SQLException { return false; }
        @Override public int getRow() throws SQLException { return indiceActual + 1; }
        @Override public boolean absolute(int row) throws SQLException { return false; }
        @Override public boolean relative(int rows) throws SQLException { return false; }
        @Override public boolean previous() throws SQLException { return false; }
        @Override public void setFetchDirection(int d) throws SQLException {}
        @Override public int getFetchDirection() throws SQLException { return FETCH_FORWARD; }
        @Override public void setFetchSize(int rows) throws SQLException {}
        @Override public int getFetchSize() throws SQLException { return 0; }
        @Override public int getType() throws SQLException { return TYPE_FORWARD_ONLY; }
        @Override public int getConcurrency() throws SQLException { return CONCUR_READ_ONLY; }
        @Override public boolean rowUpdated() throws SQLException { return false; }
        @Override public boolean rowInserted() throws SQLException { return false; }
        @Override public boolean rowDeleted() throws SQLException { return false; }
        @Override public void updateNull(int i) throws SQLException {}
        @Override public void updateBoolean(int i, boolean x) throws SQLException {}
        @Override public void updateByte(int i, byte x) throws SQLException {}
        @Override public void updateShort(int i, short x) throws SQLException {}
        @Override public void updateInt(int i, int x) throws SQLException {}
        @Override public void updateLong(int i, long x) throws SQLException {}
        @Override public void updateFloat(int i, float x) throws SQLException {}
        @Override public void updateDouble(int i, double x) throws SQLException {}
        @Override public void updateBigDecimal(int i, java.math.BigDecimal x) throws SQLException {}
        @Override public void updateString(int i, String x) throws SQLException {}
        @Override public void updateBytes(int i, byte[] x) throws SQLException {}
        @Override public void updateDate(int i, java.sql.Date x) throws SQLException {}
        @Override public void updateTime(int i, java.sql.Time x) throws SQLException {}
        @Override public void updateTimestamp(int i, Timestamp x) throws SQLException {}
        @Override public void updateAsciiStream(int i, InputStream x, int l) throws SQLException {}
        @Override public void updateBinaryStream(int i, InputStream x, int l) throws SQLException {}
        @Override public void updateCharacterStream(int i, Reader x, int l) throws SQLException {}
        @Override public void updateObject(int i, Object x, int s) throws SQLException {}
        @Override public void updateObject(int i, Object x) throws SQLException {}
        @Override public void updateNull(String c) throws SQLException {}
        @Override public void updateBoolean(String c, boolean x) throws SQLException {}
        @Override public void updateByte(String c, byte x) throws SQLException {}
        @Override public void updateShort(String c, short x) throws SQLException {}
        @Override public void updateInt(String c, int x) throws SQLException {}
        @Override public void updateLong(String c, long x) throws SQLException {}
        @Override public void updateFloat(String c, float x) throws SQLException {}
        @Override public void updateDouble(String c, double x) throws SQLException {}
        @Override public void updateBigDecimal(String c, java.math.BigDecimal x) throws SQLException {}
        @Override public void updateString(String c, String x) throws SQLException {}
        @Override public void updateBytes(String c, byte[] x) throws SQLException {}
        @Override public void updateDate(String c, java.sql.Date x) throws SQLException {}
        @Override public void updateTime(String c, java.sql.Time x) throws SQLException {}
        @Override public void updateTimestamp(String c, Timestamp x) throws SQLException {}
        @Override public void updateAsciiStream(String c, InputStream x, int l) throws SQLException {}
        @Override public void updateBinaryStream(String c, InputStream x, int l) throws SQLException {}
        @Override public void updateCharacterStream(String c, Reader x, int l) throws SQLException {}
        @Override public void updateObject(String c, Object x, int s) throws SQLException {}
        @Override public void updateObject(String c, Object x) throws SQLException {}
        @Override public void insertRow() throws SQLException {}
        @Override public void updateRow() throws SQLException {}
        @Override public void deleteRow() throws SQLException {}
        @Override public void refreshRow() throws SQLException {}
        @Override public void cancelRowUpdates() throws SQLException {}
        @Override public void moveToInsertRow() throws SQLException {}
        @Override public void moveToCurrentRow() throws SQLException {}
        @Override public Statement getStatement() throws SQLException { return null; }
        @Override public Object getObject(int i, Map<String, Class<?>> m) throws SQLException { return null; }
        @Override public Ref getRef(int i) throws SQLException { return null; }
        @Override public Blob getBlob(int i) throws SQLException { return null; }
        @Override public Clob getClob(int i) throws SQLException { return null; }
        @Override public Array getArray(int i) throws SQLException { return null; }
        @Override public Object getObject(String c, Map<String, Class<?>> m) throws SQLException { return null; }
        @Override public Ref getRef(String c) throws SQLException { return null; }
        @Override public Blob getBlob(String c) throws SQLException { return null; }
        @Override public Clob getClob(String c) throws SQLException { return null; }
        @Override public Array getArray(String c) throws SQLException { return null; }
        @Override public java.sql.Date getDate(int i, java.util.Calendar cal) throws SQLException { return null; }
        @Override public java.sql.Date getDate(String c, java.util.Calendar cal) throws SQLException { return null; }
        @Override public java.sql.Time getTime(int i, java.util.Calendar cal) throws SQLException { return null; }
        @Override public java.sql.Time getTime(String c, java.util.Calendar cal) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(int i, java.util.Calendar cal) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(String c, java.util.Calendar cal) throws SQLException { return null; }
        @Override public java.net.URL getURL(int i) throws SQLException { return null; }
        @Override public java.net.URL getURL(String c) throws SQLException { return null; }
        @Override public void updateRef(int i, Ref x) throws SQLException {}
        @Override public void updateRef(String c, Ref x) throws SQLException {}
        @Override public void updateBlob(int i, Blob x) throws SQLException {}
        @Override public void updateBlob(String c, Blob x) throws SQLException {}
        @Override public void updateClob(int i, Clob x) throws SQLException {}
        @Override public void updateClob(String c, Clob x) throws SQLException {}
        @Override public void updateArray(int i, Array x) throws SQLException {}
        @Override public void updateArray(String c, Array x) throws SQLException {}
        @Override public RowId getRowId(int i) throws SQLException { return null; }
        @Override public RowId getRowId(String c) throws SQLException { return null; }
        @Override public void updateRowId(int i, RowId x) throws SQLException {}
        @Override public void updateRowId(String c, RowId x) throws SQLException {}
        @Override public int getHoldability() throws SQLException { return HOLD_CURSORS_OVER_COMMIT; }
        @Override public boolean isClosed() throws SQLException { return false; }
        @Override public void updateNString(int i, String x) throws SQLException {}
        @Override public void updateNString(String c, String x) throws SQLException {}
        @Override public void updateNClob(int i, NClob x) throws SQLException {}
        @Override public void updateNClob(String c, NClob x) throws SQLException {}
        @Override public NClob getNClob(int i) throws SQLException { return null; }
        @Override public NClob getNClob(String c) throws SQLException { return null; }
        @Override public SQLXML getSQLXML(int i) throws SQLException { return null; }
        @Override public SQLXML getSQLXML(String c) throws SQLException { return null; }
        @Override public void updateSQLXML(int i, SQLXML x) throws SQLException {}
        @Override public void updateSQLXML(String c, SQLXML x) throws SQLException {}
        @Override public String getNString(int i) throws SQLException { return null; }
        @Override public String getNString(String c) throws SQLException { return null; }
        @Override public Reader getNCharacterStream(int i) throws SQLException { return null; }
        @Override public Reader getNCharacterStream(String c) throws SQLException { return null; }
        @Override public void updateNCharacterStream(int i, Reader x, long l) throws SQLException {}
        @Override public void updateNCharacterStream(String c, Reader x, long l) throws SQLException {}
        @Override public void updateAsciiStream(int i, InputStream x, long l) throws SQLException {}
        @Override public void updateBinaryStream(int i, InputStream x, long l) throws SQLException {}
        @Override public void updateCharacterStream(int i, Reader x, long l) throws SQLException {}
        @Override public void updateAsciiStream(String c, InputStream x, long l) throws SQLException {}
        @Override public void updateBinaryStream(String c, InputStream x, long l) throws SQLException {}
        @Override public void updateCharacterStream(String c, Reader x, long l) throws SQLException {}
        @Override public void updateBlob(int i, InputStream x, long l) throws SQLException {}
        @Override public void updateBlob(String c, InputStream x, long l) throws SQLException {}
        @Override public void updateClob(int i, Reader x, long l) throws SQLException {}
        @Override public void updateClob(String c, Reader x, long l) throws SQLException {}
        @Override public void updateNClob(int i, Reader x, long l) throws SQLException {}
        @Override public void updateNClob(String c, Reader x, long l) throws SQLException {}
        @Override public void updateNCharacterStream(int i, Reader x) throws SQLException {}
        @Override public void updateNCharacterStream(String c, Reader x) throws SQLException {}
        @Override public void updateAsciiStream(int i, InputStream x) throws SQLException {}
        @Override public void updateBinaryStream(int i, InputStream x) throws SQLException {}
        @Override public void updateCharacterStream(int i, Reader x) throws SQLException {}
        @Override public void updateAsciiStream(String c, InputStream x) throws SQLException {}
        @Override public void updateBinaryStream(String c, InputStream x) throws SQLException {}
        @Override public void updateCharacterStream(String c, Reader x) throws SQLException {}
        @Override public void updateBlob(int i, InputStream x) throws SQLException {}
        @Override public void updateBlob(String c, InputStream x) throws SQLException {}
        @Override public void updateClob(int i, Reader x) throws SQLException {}
        @Override public void updateClob(String c, Reader x) throws SQLException {}
        @Override public void updateNClob(int i, Reader x) throws SQLException {}
        @Override public void updateNClob(String c, Reader x) throws SQLException {}
        @Override public <T> T getObject(int i, Class<T> t) throws SQLException { return null; }
        @Override public <T> T getObject(String c, Class<T> t) throws SQLException { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
    }

    private List<Map<String, Object>> obtenerUsuariosParaListar() {
        try {
            List<UsuarioJson> usuarios = leerUsuariosDesdeJson();

            List<Map<String, Object>> resultado = new ArrayList<>();
            for (UsuarioJson usuario : usuarios) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("ID", usuario.UsuarioID);
                fila.put("USUARIO", usuario.Usuario);
                fila.put("NOMBRE", usuario.Nombre);
                fila.put("APELLIDO", usuario.Apellido);
                fila.put("CLAVE", usuario.Clave);
                fila.put("ROLNOMBRE", obtenerNombreRolPorId(usuario.RolID));
                fila.put("ROLID", usuario.RolID);
                fila.put("ESTADONOMBRE", obtenerNombreEstadoPorId(usuario.EstadoUID));
                resultado.add(fila);
            }
            return resultado;
        } catch (Exception e) {
            System.err.println("Error al obtener usuarios para listar: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean validarLogin(String usuario, String clave) {
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson();
        for (UsuarioJson u : usuarios) {
            if (u.Usuario != null && u.Usuario.equals(usuario)
                    && u.Clave != null && u.Clave.equals(clave)
                    && u.EstadoUID == 1) {
                return true;
            }
        }
        return false;
    }

    public UsuarioLoginResult obtenerDatosUsuarioParaLogin(String usuario) {
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson();
        for (UsuarioJson u : usuarios) {
            if (u.Usuario != null && u.Usuario.equals(usuario) && u.EstadoUID == 1) {
                UsuarioLoginResult res = new UsuarioLoginResult();
                res.usuarioID = u.UsuarioID;
                res.rol = obtenerNombreRolPorId(u.RolID);
                return res;
            }
        }
        return null;
    }

    public ResultSet consultarRoles() {
        List<RolJson> roles = leerRolesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (RolJson r : roles) {
            filas.add(new String[]{String.valueOf(r.RolID), r.Rol});
        }
        return new SimpleResultSet(filas, 2);
    }

    public ResultSet consultarAptitudes() {
        List<AptitudJson> aptitudes = leerAptitudesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (AptitudJson a : aptitudes) {
            filas.add(new String[]{String.valueOf(a.AptitudID), a.Aptitud});
        }
        return new SimpleResultSet(filas, 2);
    }

    public ResultSet listarEstadosMision() throws SQLException {
        List<EstadoMisionJson> estados = leerEstadosMisionDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (EstadoMisionJson e : estados) {
            filas.add(new String[]{String.valueOf(e.EstadoMID), e.Estado});
        }
        return new SimpleResultSet(filas, 2);
    }

    public ResultSet listarEstadosTripulante() {
        List<EstadoTripulanteJson> estados = leerEstadosTripulantesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (EstadoTripulanteJson e : estados) {
            filas.add(new String[]{String.valueOf(e.EstadoTID), e.Estado});
        }
        return new SimpleResultSet(filas, 2);
    }

    public ResultSet listarEstadosEvento() throws SQLException {
        List<EstadoEventoJson> estados = leerEstadosEventoDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (EstadoEventoJson e : estados) {
            filas.add(new String[]{String.valueOf(e.EstadoEID), e.Estado});
        }
        return new SimpleResultSet(filas, 2);
    }

    public void registrarLog(int usuarioID, int accionID, int tipoEntidadID, int entidadID, String descripcion) {
        try {
            registroLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Registros.json");
                List<RegistroJson> registros = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<RegistroJson>>() {});

                int nuevoId = 1;
                for (RegistroJson r : registros) {
                    if (r.RegistroID >= nuevoId) nuevoId = r.RegistroID + 1;
                }

                RegistroJson nuevo = new RegistroJson();
                nuevo.RegistroID = nuevoId;
                nuevo.AccionMID = accionID;
                nuevo.UsuarioID = usuarioID;
                nuevo.TipoEntidadID = tipoEntidadID;
                nuevo.EntidadID = entidadID;
                nuevo.FechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                nuevo.Descripcion = descripcion != null ? descripcion : "";

                registros.add(nuevo);
                escribirRegistrosEnJsonSinLock(registros);
            } finally {
                registroLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al registrar log: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al registrar log: " + e.getMessage());
        }
    }

    public int obtenerUsuarioID(String usuario) {
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson();
        for (UsuarioJson u : usuarios) {
            if (u.Usuario != null && u.Usuario.equals(usuario) && u.EstadoUID == 1) {
                return u.UsuarioID;
            }
        }
        throw new IllegalArgumentException("Usuario no encontrado: " + usuario);
    }

    public int registrarUsuario(int rolID, String usuario, String nombre, String apellido, String clave) {
        try {
            jsonLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<UsuarioJson> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<UsuarioJson>>() {});

                int nuevoId = 1;
                for (UsuarioJson u : usuarios) {
                    if (u.UsuarioID >= nuevoId) nuevoId = u.UsuarioID + 1;
                }

                UsuarioJson nuevoUsuario = new UsuarioJson();
                nuevoUsuario.UsuarioID = nuevoId;
                nuevoUsuario.RolID = rolID;
                nuevoUsuario.EstadoUID = 1;
                nuevoUsuario.Nombre = nombre;
                nuevoUsuario.Apellido = apellido;
                nuevoUsuario.Usuario = usuario;
                nuevoUsuario.Clave = clave;

                usuarios.add(nuevoUsuario);
                escribirUsuariosEnJsonSinLock(usuarios);
                return nuevoId;
            } finally {
                jsonLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al registrar usuario: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
        }
        return -1;
    }

    public void modificarUsuario(int usuarioIDLogueado, int usuarioID, int rolID, String usuario, String nombre, String apellido, String clave) {
        try {
            jsonLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<UsuarioJson> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<UsuarioJson>>() {});
                boolean encontrado = false;
                StringBuilder desc = new StringBuilder("Modificacion: ");

                for (UsuarioJson u : usuarios) {
                    if (u.UsuarioID == usuarioID) {
                        if (rolID != 0 && u.RolID != rolID) {
                            desc.append("Rol: '").append(obtenerNombreRolPorId(u.RolID)).append("' -> '").append(obtenerNombreRolPorId(rolID)).append("'; ");
                            u.RolID = rolID;
                        }
                        if (usuario != null && !usuario.isEmpty() && !usuario.equals(u.Usuario)) {
                            desc.append("Usuario: '").append(u.Usuario).append("' -> '").append(usuario).append("'; ");
                            u.Usuario = usuario;
                        }
                        if (nombre != null && !nombre.isEmpty() && !nombre.equals(u.Nombre)) {
                            desc.append("Nombre: '").append(u.Nombre).append("' -> '").append(nombre).append("'; ");
                            u.Nombre = nombre;
                        }
                        if (apellido != null && !apellido.isEmpty() && !apellido.equals(u.Apellido)) {
                            desc.append("Apellido: '").append(u.Apellido).append("' -> '").append(apellido).append("'; ");
                            u.Apellido = apellido;
                        }
                        if (clave != null && !clave.isEmpty() && !clave.equals(u.Clave)) {
                            desc.append("Clave: '").append(u.Clave).append("' -> '").append(clave).append("'; ");
                            u.Clave = clave;
                        }
                        encontrado = true;
                        break;
                    }
                }

                if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
                escribirUsuariosEnJsonSinLock(usuarios);
                String descStr = desc.length() > 16 ? desc.substring(0, desc.length() - 2) : "Sin cambios";
                registrarLog(usuarioIDLogueado, 14, 4, usuarioID, descStr);
            } finally {
                jsonLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al modificar usuario: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al modificar usuario: " + e.getMessage());
        }
    }

    public String obtenerClaveUsuario(String usuario) {
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson();
        for (UsuarioJson u : usuarios) {
            if (u.Usuario != null && u.Usuario.equals(usuario) && u.EstadoUID == 1) {
                return u.Clave;
            }
        }
        return "";
    }

    public void bajaUsuario(int usuarioIDLogueado, int usuarioID) {
        try {
            jsonLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<UsuarioJson> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<UsuarioJson>>() {});
                boolean encontrado = false;

                for (UsuarioJson u : usuarios) {
                    if (u.UsuarioID == usuarioID) {
                        String estadoAnterior = obtenerNombreEstadoPorId(u.EstadoUID);
                        u.EstadoUID = 2;
                        String estadoActual = obtenerNombreEstadoPorId(u.EstadoUID);
                        escribirUsuariosEnJsonSinLock(usuarios);
                        String desc = "Baja logica: Estado: '" + estadoAnterior + "' -> '" + estadoActual + "'";
                        registrarLog(usuarioIDLogueado, 14, 4, usuarioID, desc);
                        encontrado = true;
                        break;
                    }
                }

                if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
            } finally {
                jsonLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al dar de baja usuario: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al dar de baja usuario: " + e.getMessage());
        }
    }

    public void bajaUsuario(int usuarioIDLogueado, String usuarioOID) {
        try {
            bajaUsuario(usuarioIDLogueado, Integer.parseInt(usuarioOID));
        } catch (NumberFormatException e) {
            try {
                jsonLock.iniciarEscritura();
                try {
                    Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                    List<UsuarioJson> usuarios = mapper.readValue(ruta.toFile(),
                            new TypeReference<List<UsuarioJson>>() {});
                    boolean encontrado = false;
                    for (UsuarioJson u : usuarios) {
                        if (u.Usuario != null && u.Usuario.equals(usuarioOID)) {
                            String estadoAnterior = obtenerNombreEstadoPorId(u.EstadoUID);
                            u.EstadoUID = 2;
                            String estadoActual = obtenerNombreEstadoPorId(u.EstadoUID);
                            escribirUsuariosEnJsonSinLock(usuarios);
                            String desc = "Baja logica: Estado: '" + estadoAnterior + "' -> '" + estadoActual + "'";
                            registrarLog(usuarioIDLogueado, 14, 4, u.UsuarioID, desc);
                            encontrado = true;
                            break;
                        }
                    }
                    if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado: " + usuarioOID);
                } finally {
                    jsonLock.terminarEscritura();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("Operación interrumpida al dar de baja usuario: " + ie.getMessage());
            } catch (IOException ioe) {
                System.err.println("Error al dar de baja usuario: " + ioe.getMessage());
            }
        }
    }

    public ResultSet listarUsuarios() throws SQLException {
        return new UsuarioResultSet(obtenerUsuariosParaListar());
    }

    public int registrarMision(int estadoMID, String nombre, String descripcion, Timestamp ini, Timestamp fin) {
        try {
            misionLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
                List<MisionJson> misiones = mapper.readValue(ruta.toFile(), new TypeReference<List<MisionJson>>() {});

                int nuevoId = 1;
                for (MisionJson m : misiones) {
                    if (m.MisionID >= nuevoId) nuevoId = m.MisionID + 1;
                }

                MisionJson nueva = new MisionJson();
                nueva.MisionID = nuevoId;
                nueva.EstadoMID = estadoMID;
                nueva.Nombre = nombre;
                nueva.Descripcion = descripcion;
                nueva.FechaInicioEstimada = tsToString(ini);
                nueva.FechaFinEstimada = tsToString(fin);
                nueva.RetrasoInicio = null;
                nueva.RetrasoFin = null;

                misiones.add(nueva);
                escribirMisionesEnJsonSinLock(misiones);
                return nuevoId;
            } finally {
                misionLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al registrar misión: " + e.getMessage());
        }
        return -1;
    }

    public void modificarMision(int usuarioIDLogueado, int id, String nombre, String desc, Timestamp ini, Timestamp fin) {
        try {
            misionLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
                List<MisionJson> misiones = mapper.readValue(ruta.toFile(), new TypeReference<List<MisionJson>>() {});
                StringBuilder descChanges = new StringBuilder("Modificacion: ");
                for (MisionJson m : misiones) {
                    if (m.MisionID == id) {
                        if (nombre != null && !nombre.equals(m.Nombre)) {
                            descChanges.append("Nombre: '").append(m.Nombre).append("' -> '").append(nombre).append("'; ");
                            m.Nombre = nombre;
                        }
                        if (desc != null && !desc.equals(m.Descripcion)) {
                            descChanges.append("Descripcion: '").append(m.Descripcion).append("' -> '").append(desc).append("'; ");
                            m.Descripcion = desc;
                        }
                        String newIni = tsToString(ini);
                        if (newIni != null && !newIni.equals(m.FechaInicioEstimada)) {
                            descChanges.append("FechaInicio: '").append(m.FechaInicioEstimada).append("' -> '").append(newIni).append("'; ");
                            m.FechaInicioEstimada = newIni;
                        }
                        String newFin = tsToString(fin);
                        if (newFin != null && !newFin.equals(m.FechaFinEstimada)) {
                            descChanges.append("FechaFin: '").append(m.FechaFinEstimada).append("' -> '").append(newFin).append("'; ");
                            m.FechaFinEstimada = newFin;
                        }
                        break;
                    }
                }
                escribirMisionesEnJsonSinLock(misiones);
                String descStr = descChanges.length() > 16 ? descChanges.substring(0, descChanges.length() - 2) : "Sin cambios";
                registrarLog(usuarioIDLogueado, 2, 1, id, descStr);
            } finally {
                misionLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al modificar misión: " + e.getMessage());
        }
    }

    public void actualizarEstadoMision(int usuarioIDLogueado, int id, int estadoID, Integer retrasoInicio, Integer retrasoFin) {
        try {
            misionLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Misiones.json");
                List<MisionJson> misiones = mapper.readValue(ruta.toFile(), new TypeReference<List<MisionJson>>() {});
                int accionID = 0;
                for (MisionJson m : misiones) {
                    if (m.MisionID == id) {
                        String estadoAnterior = obtenerNombreEstadoMisionPorId(m.EstadoMID);
                        m.EstadoMID = estadoID;
                        if (retrasoInicio != null) m.RetrasoInicio = retrasoInicio;
                        if (retrasoFin != null) m.RetrasoFin = retrasoFin;
                        String estadoNuevo = obtenerNombreEstadoMisionPorId(estadoID);
                        if (estadoID == 5) accionID = 3;
                        else if (estadoID == 4) accionID = 4;
                        else accionID = 2;
                        String desc = "Estado: '" + estadoAnterior + "' -> '" + estadoNuevo + "'";
                        escribirMisionesEnJsonSinLock(misiones);
                        registrarLog(usuarioIDLogueado, accionID, 1, id, desc);
                        return;
                    }
                }
                escribirMisionesEnJsonSinLock(misiones);
            } finally {
                misionLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al actualizar estado de misión: " + e.getMessage());
        }
    }

    public ResultSet listarMisiones() throws SQLException {
        List<MisionJson> misiones = leerMisionesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (MisionJson m : misiones) {
            filas.add(new String[]{
                String.valueOf(m.MisionID),
                m.Nombre != null ? m.Nombre : "",
                m.FechaInicioEstimada != null ? m.FechaInicioEstimada : "",
                m.FechaFinEstimada != null ? m.FechaFinEstimada : "",
                m.RetrasoInicio != null ? String.valueOf(m.RetrasoInicio) : "",
                m.RetrasoFin != null ? String.valueOf(m.RetrasoFin) : "",
                obtenerNombreEstadoMisionPorId(m.EstadoMID)
            });
        }
        return new SimpleResultSet(filas, 7);
    }

    public ResultSet consultarMision(int id) throws SQLException {
        List<MisionJson> misiones = leerMisionesDesdeJson();
        for (MisionJson m : misiones) {
            if (m.MisionID == id) {
                List<String[]> filas = new ArrayList<>();
                filas.add(new String[]{
                    String.valueOf(m.MisionID),
                    m.Nombre != null ? m.Nombre : "",
                    m.Descripcion != null ? m.Descripcion : "",
                    obtenerNombreEstadoMisionPorId(m.EstadoMID),
                    m.FechaInicioEstimada != null ? m.FechaInicioEstimada : "",
                    m.FechaFinEstimada != null ? m.FechaFinEstimada : "",
                    m.RetrasoInicio != null ? String.valueOf(m.RetrasoInicio) : "",
                    m.RetrasoFin != null ? String.valueOf(m.RetrasoFin) : ""
                });
                return new SimpleResultSet(filas, 8);
            }
        }
        return new SimpleResultSet(new ArrayList<>(), 8);
    }

    public boolean existeMision(int id) throws SQLException {
        List<MisionJson> misiones = leerMisionesDesdeJson();
        for (MisionJson m : misiones) {
            if (m.MisionID == id) return true;
        }
        return false;
    }

    public ResultSet registrarTripulante(int estadoTID, int sexoID, int peso, int altura,
            String nombre, String apellido, String imagen, Date fechaNacimiento) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                List<TripulanteJson> tripulantes = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<TripulanteJson>>() {});

                int nuevoId = 1;
                for (TripulanteJson t : tripulantes) {
                    if (t.TripulanteID >= nuevoId) nuevoId = t.TripulanteID + 1;
                }

                TripulanteJson nuevo = new TripulanteJson();
                nuevo.TripulanteID = nuevoId;
                nuevo.EstadoTID = estadoTID;
                nuevo.SexoID = sexoID;
                nuevo.Peso = peso;
                nuevo.Altura = altura;
                nuevo.Nombre = nombre;
                nuevo.Apellido = apellido;
                nuevo.Imagen = imagen;
                nuevo.FechaDeNacimiento = fechaNacimiento.toString();

                tripulantes.add(nuevo);
                escribirTripulantesEnJsonSinLock(tripulantes);

                List<String[]> filas = new ArrayList<>();
                filas.add(new String[]{String.valueOf(nuevoId)});
                return new SimpleResultSet(filas, 1);
            } finally {
                tripulanteLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            List<String[]> filas = new ArrayList<>();
            filas.add(new String[]{"-1"});
            return new SimpleResultSet(filas, 1);
        } catch (IOException e) {
            System.err.println("Error al registrar tripulante: " + e.getMessage());
            return new SimpleResultSet(new ArrayList<>(), 1);
        }
    }

    public void modificarTripulante(int usuarioIDLogueado, int tripulanteID, int estadoTID, int sexoID, int peso, int altura,
            String nombre, String apellido, String imagen, Date fechaNacimiento) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                List<TripulanteJson> tripulantes = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<TripulanteJson>>() {});
                StringBuilder desc = new StringBuilder("Modificacion: ");

                for (TripulanteJson t : tripulantes) {
                    if (t.TripulanteID == tripulanteID) {
                        if (t.EstadoTID != estadoTID) {
                            desc.append("Estado: '").append(obtenerNombreEstadoTripulantePorId(t.EstadoTID)).append("' -> '").append(obtenerNombreEstadoTripulantePorId(estadoTID)).append("'; ");
                            t.EstadoTID = estadoTID;
                        }
                        if (t.SexoID != sexoID) {
                            desc.append("Sexo: '").append(obtenerNombreSexoPorId(t.SexoID)).append("' -> '").append(obtenerNombreSexoPorId(sexoID)).append("'; ");
                            t.SexoID = sexoID;
                        }
                        if (t.Peso != peso) {
                            desc.append("Peso: '").append(t.Peso).append("' -> '").append(peso).append("'; ");
                            t.Peso = peso;
                        }
                        if (t.Altura != altura) {
                            desc.append("Altura: '").append(t.Altura).append("' -> '").append(altura).append("'; ");
                            t.Altura = altura;
                        }
                        if (nombre != null && !nombre.equals(t.Nombre)) {
                            desc.append("Nombre: '").append(t.Nombre).append("' -> '").append(nombre).append("'; ");
                            t.Nombre = nombre;
                        }
                        if (apellido != null && !apellido.equals(t.Apellido)) {
                            desc.append("Apellido: '").append(t.Apellido).append("' -> '").append(apellido).append("'; ");
                            t.Apellido = apellido;
                        }
                        if (imagen != null && !imagen.equals(t.Imagen)) {
                            desc.append("Imagen: '").append(t.Imagen).append("' -> '").append(imagen).append("'; ");
                            t.Imagen = imagen;
                        }
                        String newFecha = fechaNacimiento.toString();
                        if (!newFecha.equals(t.FechaDeNacimiento)) {
                            desc.append("FechaNacimiento: '").append(t.FechaDeNacimiento).append("' -> '").append(newFecha).append("'; ");
                            t.FechaDeNacimiento = newFecha;
                        }
                        break;
                    }
                }

                escribirTripulantesEnJsonSinLock(tripulantes);
                String descStr = desc.length() > 16 ? desc.substring(0, desc.length() - 2) : "Sin cambios";
                registrarLog(usuarioIDLogueado, 9, 2, tripulanteID, descStr);
            } finally {
                tripulanteLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al modificar tripulante: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al modificar tripulante: " + e.getMessage());
        }
    }

    public void bajaTripulante(int usuarioIDLogueado, int tripulanteID) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                List<TripulanteJson> tripulantes = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<TripulanteJson>>() {});

                for (TripulanteJson t : tripulantes) {
                    if (t.TripulanteID == tripulanteID) {
                        String estadoAnterior = obtenerNombreEstadoTripulantePorId(t.EstadoTID);
                        t.EstadoTID = 3;
                        String estadoActual = obtenerNombreEstadoTripulantePorId(t.EstadoTID);
                        escribirTripulantesEnJsonSinLock(tripulantes);
                        String desc = "Baja logica: Estado: '" + estadoAnterior + "' -> '" + estadoActual + "'";
                        registrarLog(usuarioIDLogueado, 10, 2, tripulanteID, desc);
                        return;
                    }
                }

                escribirTripulantesEnJsonSinLock(tripulantes);
            } finally {
                tripulanteLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al dar de baja tripulante: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al dar de baja tripulante: " + e.getMessage());
        }
    }

    public void asignarTripulante(int usuarioIDLogueado, int tripID, int misID, Timestamp fecha) {
        try {
            misionLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "GrupoMisiones.json");
                List<GrupoMisionJson> grupos = mapper.readValue(ruta.toFile(), new TypeReference<List<GrupoMisionJson>>() {});
                GrupoMisionJson nuevo = new GrupoMisionJson();
                nuevo.TripulanteID = tripID;
                nuevo.MisionID = misID;
                nuevo.FechaAsignacion = tsToString(fecha);
                grupos.add(nuevo);
                escribirGrupoMisionesEnJsonSinLock(grupos);
                String descLog = "Asignacion: TripulanteID=[" + tripID + "], MisionID=[" + misID + "]";
                registrarLog(usuarioIDLogueado, 5, 1, misID, descLog);
            } finally {
                misionLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al asignar tripulante a misión: " + e.getMessage());
        }
    }

    public ResultSet listarTripulantes() {
        List<TripulanteJson> tripulantes = leerTripulantesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (TripulanteJson t : tripulantes) {
            filas.add(new String[]{
                String.valueOf(t.TripulanteID),
                t.Nombre != null ? t.Nombre : "",
                t.Apellido != null ? t.Apellido : "",
                t.Imagen != null ? t.Imagen : "",
                obtenerNombreEstadoTripulantePorId(t.EstadoTID),
                obtenerNombreSexoPorId(t.SexoID),
                String.valueOf(t.Peso),
                String.valueOf(t.Altura)
            });
        }
        return new SimpleResultSet(filas, 8);
    }

    public ResultSet listarTripulantesMision(int misionID) throws SQLException {
        List<GrupoMisionJson> grupos = leerGrupoMisionesDesdeJson();
        List<TripulanteJson> tripulantes = leerTripulantesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (GrupoMisionJson g : grupos) {
            if (g.MisionID == misionID) {
                for (TripulanteJson t : tripulantes) {
                    if (t.TripulanteID == g.TripulanteID) {
                        filas.add(new String[]{
                            String.valueOf(t.TripulanteID),
                            t.Nombre != null ? t.Nombre : "",
                            t.Apellido != null ? t.Apellido : "",
                            obtenerNombreEstadoTripulantePorId(t.EstadoTID),
                            g.FechaAsignacion != null ? g.FechaAsignacion : ""
                        });
                        break;
                    }
                }
            }
        }
        return new SimpleResultSet(filas, 5);
    }

    public ResultSet listarMisionesTripulante(int tripulanteID) throws SQLException {
        List<GrupoMisionJson> grupos = leerGrupoMisionesDesdeJson();
        List<MisionJson> misiones = leerMisionesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (GrupoMisionJson g : grupos) {
            if (g.TripulanteID == tripulanteID) {
                for (MisionJson m : misiones) {
                    if (m.MisionID == g.MisionID) {
                        filas.add(new String[]{
                            String.valueOf(m.MisionID),
                            m.Nombre != null ? m.Nombre : "",
                            obtenerNombreEstadoMisionPorId(m.EstadoMID),
                            m.FechaInicioEstimada != null ? m.FechaInicioEstimada : "",
                            m.FechaFinEstimada != null ? m.FechaFinEstimada : ""
                        });
                        break;
                    }
                }
            }
        }
        return new SimpleResultSet(filas, 5);
    }

    public ResultSet consultarTripulante(int tripulanteID) {
        List<TripulanteJson> tripulantes = leerTripulantesDesdeJson();
        for (TripulanteJson t : tripulantes) {
            if (t.TripulanteID == tripulanteID) {
                List<String[]> filas = new ArrayList<>();
                filas.add(new String[]{
                    String.valueOf(t.TripulanteID),
                    t.Nombre != null ? t.Nombre : "",
                    t.Apellido != null ? t.Apellido : "",
                    String.valueOf(t.Peso),
                    String.valueOf(t.Altura),
                    t.Imagen != null ? t.Imagen : "",
                    t.FechaDeNacimiento != null ? t.FechaDeNacimiento : "",
                    obtenerNombreEstadoTripulantePorId(t.EstadoTID),
                    obtenerNombreSexoPorId(t.SexoID)
                });
                return new SimpleResultSet(filas, 9);
            }
        }
        return new SimpleResultSet(new ArrayList<>(), 9);
    }

    public boolean existeTripulante(int id) {
        List<TripulanteJson> tripulantes = leerTripulantesDesdeJson();
        for (TripulanteJson t : tripulantes) {
            if (t.TripulanteID == id) return true;
        }
        return false;
    }

    public ResultSet consultarCapacidades(int tripulanteID) {
        List<CapacidadJson> capacidades = leerCapacidadesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (CapacidadJson c : capacidades) {
            if (c.TripulanteID == tripulanteID) {
                filas.add(new String[]{
                    String.valueOf(c.AptitudID),
                    obtenerNombreAptitudPorId(c.AptitudID),
                    String.valueOf(c.Calificacion),
                    c.FechaCapacidades != null ? c.FechaCapacidades : ""
                });
            }
        }
        return new SimpleResultSet(filas, 4);
    }

    public void registrarCapacidad(int usuarioIDLogueado, int tripulanteID, int aptitudID, int calificacion, String fecha) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                List<CapacidadJson> capacidades = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<CapacidadJson>>() {});

                boolean existe = false;
                for (CapacidadJson c : capacidades) {
                    if (c.TripulanteID == tripulanteID && c.AptitudID == aptitudID) {
                        String descFecha = "Calificacion: '" + c.Calificacion + "' -> '" + calificacion + "'";
                        c.Calificacion = calificacion;
                        c.FechaCapacidades = fecha;
                        escribirCapacidadesEnJsonSinLock(capacidades);
                        registrarLog(usuarioIDLogueado, 12, 5, tripulanteID, descFecha);
                        existe = true;
                        return;
                    }
                }

                if (!existe) {
                    CapacidadJson nueva = new CapacidadJson();
                    nueva.TripulanteID = tripulanteID;
                    nueva.AptitudID = aptitudID;
                    nueva.Calificacion = calificacion;
                    nueva.FechaCapacidades = fecha;

                    capacidades.add(nueva);
                    escribirCapacidadesEnJsonSinLock(capacidades);
                    String descLog = "Alta: TripulanteID=[" + tripulanteID + "], AptitudID=[" + aptitudID + "], Calificacion=[" + calificacion + "]";
                    registrarLog(usuarioIDLogueado, 11, 5, tripulanteID, descLog);
                }
            } finally {
                tripulanteLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al registrar capacidad: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al registrar capacidad: " + e.getMessage());
        }
    }

    public void eliminarCapacidades(int tripulanteID) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                List<CapacidadJson> capacidades = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<CapacidadJson>>() {});
                capacidades.removeIf(c -> c.TripulanteID == tripulanteID);
                escribirCapacidadesEnJsonSinLock(capacidades);
            } finally {
                tripulanteLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al eliminar capacidades: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al eliminar capacidades: " + e.getMessage());
        }
    }

    public int registrarEvento(int usuarioIDLogueado, int misionID, String titulo, String desc, Timestamp fecha) {
        try {
            eventoLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Eventos.json");
                List<EventoJson> eventos = mapper.readValue(ruta.toFile(), new TypeReference<List<EventoJson>>() {});

                int nuevoId = 1;
                for (EventoJson e : eventos) {
                    if (e.EventoID >= nuevoId) nuevoId = e.EventoID + 1;
                }

                EventoJson nuevo = new EventoJson();
                nuevo.EventoID = nuevoId;
                nuevo.MisionID = misionID;
                nuevo.Titulo = titulo;
                nuevo.Fecha = tsToString(fecha);
                nuevo.Descripcion = desc;
                nuevo.EstadoEID = 1;

                eventos.add(nuevo);
                escribirEventosEnJsonSinLock(eventos);
                String descLog = "Alta: MisionID=[" + misionID + "], Titulo=[" + titulo + "], Descripcion=[" + desc + "]";
                registrarLog(usuarioIDLogueado, 6, 3, nuevoId, descLog);
                return nuevoId;
            } finally {
                eventoLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al registrar evento: " + e.getMessage());
        }
        return -1;
    }

    public void bajaEvento(int usuarioIDLogueado, int eventoID) {
        try {
            eventoLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Eventos.json");
                List<EventoJson> eventos = mapper.readValue(ruta.toFile(), new TypeReference<List<EventoJson>>() {});
                for (EventoJson e : eventos) {
                    if (e.EventoID == eventoID) {
                        String estadoAnterior = obtenerNombreEstadoEventoPorId(e.EstadoEID);
                        e.EstadoEID = 2;
                        String estadoActual = obtenerNombreEstadoEventoPorId(e.EstadoEID);
                        escribirEventosEnJsonSinLock(eventos);
                        String desc = "Baja logica: Estado: '" + estadoAnterior + "' -> '" + estadoActual + "'";
                        registrarLog(usuarioIDLogueado, 7, 3, eventoID, desc);
                        return;
                    }
                }
                escribirEventosEnJsonSinLock(eventos);
            } finally {
                eventoLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al desestimar evento: " + e.getMessage());
        }
    }

    public ResultSet listarEventos() throws SQLException {
        List<EventoJson> eventos = leerEventosDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (EventoJson e : eventos) {
            filas.add(new String[]{
                String.valueOf(e.EventoID),
                obtenerNombreMisionPorId(e.MisionID),
                e.Titulo != null ? e.Titulo : "",
                e.Fecha != null ? e.Fecha : "",
                e.Descripcion != null ? e.Descripcion : "",
                obtenerNombreEstadoEventoPorId(e.EstadoEID)
            });
        }
        return new SimpleResultSet(filas, 6);
    }

    public ResultSet consultarEventos(int misionID) throws SQLException {
        List<EventoJson> eventos = leerEventosDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (EventoJson e : eventos) {
            if (e.MisionID == misionID) {
                filas.add(new String[]{
                    String.valueOf(e.EventoID),
                    e.Titulo != null ? e.Titulo : "",
                    e.Fecha != null ? e.Fecha : "",
                    e.Descripcion != null ? e.Descripcion : "",
                    obtenerNombreEstadoEventoPorId(e.EstadoEID)
                });
            }
        }
        return new SimpleResultSet(filas, 5);
    }

    public ResultSet verLogs() {
        List<RegistroJson> registros = leerRegistrosDesdeJson();
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson();
        List<RolJson> roles = leerRolesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (RegistroJson r : registros) {
            String nombreUsuario = "";
            String nombreRol = "";
            for (UsuarioJson u : usuarios) {
                if (u.UsuarioID == r.UsuarioID) {
                    nombreUsuario = u.Usuario != null ? u.Usuario : "";
                    for (RolJson rol : roles) {
                        if (rol.RolID == u.RolID) {
                            nombreRol = rol.Rol;
                            break;
                        }
                    }
                    break;
                }
            }
            String nombreAccion = obtenerNombreAccionPorId(r.AccionMID);
            String nombreEntidad = obtenerNombreEntidadPorId(r.TipoEntidadID);
            filas.add(new String[]{
                String.valueOf(r.RegistroID),
                nombreUsuario,
                nombreRol,
                nombreAccion != null ? nombreAccion : "",
                nombreEntidad != null ? nombreEntidad : "",
                String.valueOf(r.EntidadID),
                r.FechaHora != null ? r.FechaHora : "",
                r.Descripcion != null ? r.Descripcion : ""
            });
        }
        filas.sort((a, b) -> b[6].compareTo(a[6]));
        return new SimpleResultSet(filas, 8);
    }
}
