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
import java.sql.Connection;
import java.sql.CallableStatement;
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
import java.util.Date;
import java.util.Collections;

public class AccesoDatos {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final SemaforoRW jsonLock = new SemaforoRW();
    private static final SemaforoRW tripulanteLock = new SemaforoRW();

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

    // --- Tripulantes: lectura/escritura JSON ---
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

    // --- Capacidades: lectura/escritura JSON ---
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

    // --- Referencias: solo lectura desde JSON ---
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

    // --- ResultSet generico para consultas desde JSON ---
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

    public ResultSet obtenerDatosUsuario(String usuario) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarUsuario(?)}");
        cs.setString(1, usuario);
        return cs.executeQuery();
    }

    public ResultSet consultarRoles() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarRoles()}");
        return cs.executeQuery();
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
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarEstadosMisiones()}");
        return cs.executeQuery();
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
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarEstadosEventos()}");
        return cs.executeQuery();
    }

    public void registrarLog(int usuarioID, int accionID, int tipoEntidadID, int entidadID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ARegistro(?, ?, ?, ?, ?)}");
        cs.setInt(1, usuarioID);
        cs.setInt(2, accionID);
        cs.setInt(3, tipoEntidadID);
        cs.setInt(4, entidadID);
        cs.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
        cs.execute();
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

    public void registrarUsuario(int rolID, String usuario, String nombre, String apellido, String clave) {
        try {
            jsonLock.iniciarEscritura();
            try {
                // Leer dentro del lock de escritura: ningún otro hilo puede leer ni escribir.
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
            } finally {
                jsonLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al registrar usuario: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
        }
    }

    public void modificarUsuario(int usuarioID, int rolID, String usuario, String nombre, String apellido, String clave) {
        try {
            jsonLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<UsuarioJson> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<UsuarioJson>>() {});
                boolean encontrado = false;

                for (UsuarioJson u : usuarios) {
                    if (u.UsuarioID == usuarioID) {
                        if (rolID != 0) u.RolID = rolID;
                        if (usuario != null && !usuario.isEmpty()) u.Usuario = usuario;
                        if (nombre != null && !nombre.isEmpty()) u.Nombre = nombre;
                        if (apellido != null && !apellido.isEmpty()) u.Apellido = apellido;
                        if (clave != null && !clave.isEmpty()) u.Clave = clave;
                        encontrado = true;
                        break;
                    }
                }

                if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
                escribirUsuariosEnJsonSinLock(usuarios);
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

    public void bajaUsuario(int usuarioID) {
        try {
            jsonLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<UsuarioJson> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<UsuarioJson>>() {});
                boolean encontrado = false;

                for (UsuarioJson u : usuarios) {
                    if (u.UsuarioID == usuarioID) {
                        u.EstadoUID = 2; // baja lógica → Inactivo
                        encontrado = true;
                        break;
                    }
                }

                if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
                escribirUsuariosEnJsonSinLock(usuarios);
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

    public void bajaUsuario(String usuarioOID) {
        try {
            bajaUsuario(Integer.parseInt(usuarioOID));
        } catch (NumberFormatException e) {
            // Búsqueda por nombre de usuario (flujo legacy)
            try {
                jsonLock.iniciarEscritura();
                try {
                    Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                    List<UsuarioJson> usuarios = mapper.readValue(ruta.toFile(),
                            new TypeReference<List<UsuarioJson>>() {});
                    boolean encontrado = false;
                    for (UsuarioJson u : usuarios) {
                        if (u.Usuario != null && u.Usuario.equals(usuarioOID)) {
                            u.EstadoUID = 2;
                            encontrado = true;
                            break;
                        }
                    }
                    if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado: " + usuarioOID);
                    escribirUsuariosEnJsonSinLock(usuarios);
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

    public void registrarMision(int estadoMID, String nombre, String descripcion, Timestamp ini, Timestamp fin) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL AMision(?, ?, ?, ?, ?)}");
        cs.setInt(1, estadoMID);
        cs.setString(2, nombre);
        cs.setString(3, descripcion);
        cs.setTimestamp(4, ini);
        cs.setTimestamp(5, fin);
        cs.execute();
    }

    public void modificarMision(int id, String nombre, String desc, Timestamp ini, Timestamp fin) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL MMision(?, ?, ?, ?, ?)}");
        cs.setInt(1, id);
        cs.setString(2, nombre);
        cs.setString(3, desc);
        cs.setTimestamp(4, ini);
        cs.setTimestamp(5, fin);
        cs.execute();
    }

    public void actualizarEstadoMision(int id, int estadoID, Integer retrasoInicio, Integer retrasoFin) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ActualizarEstadoMision(?, ?, ?, ?)}");
        cs.setInt(1, id);
        cs.setInt(2, estadoID);
        if (retrasoInicio != null) cs.setInt(3, retrasoInicio); else cs.setNull(3, java.sql.Types.INTEGER);
        if (retrasoFin != null) cs.setInt(4, retrasoFin); else cs.setNull(4, java.sql.Types.INTEGER);
        cs.execute();
    }

    public ResultSet listarMisiones() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarMisiones()}");
        return cs.executeQuery();
    }

    public ResultSet consultarMision(int id) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarMision(?)}");
        cs.setInt(1, id);
        return cs.executeQuery();
    }

    public boolean existeMision(int id) throws SQLException {
        return consultarMision(id).next();
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

    public void modificarTripulante(int tripulanteID, int estadoTID, int sexoID, int peso, int altura,
            String nombre, String apellido, String imagen, Date fechaNacimiento) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                List<TripulanteJson> tripulantes = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<TripulanteJson>>() {});

                for (TripulanteJson t : tripulantes) {
                    if (t.TripulanteID == tripulanteID) {
                        t.EstadoTID = estadoTID;
                        t.SexoID = sexoID;
                        t.Peso = peso;
                        t.Altura = altura;
                        t.Nombre = nombre;
                        t.Apellido = apellido;
                        t.Imagen = imagen;
                        t.FechaDeNacimiento = fechaNacimiento.toString();
                        break;
                    }
                }

                escribirTripulantesEnJsonSinLock(tripulantes);
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

    public void bajaTripulante(int tripulanteID) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Tripulantes.json");
                List<TripulanteJson> tripulantes = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<TripulanteJson>>() {});

                for (TripulanteJson t : tripulantes) {
                    if (t.TripulanteID == tripulanteID) {
                        t.EstadoTID = 3; // Retirado
                        break;
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

    public void asignarTripulante(int tripID, int misID, Timestamp fecha) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL AGrupoMision(?, ?, ?)}");
        cs.setInt(1, tripID);
        cs.setInt(2, misID);
        cs.setTimestamp(3, fecha);
        cs.execute();
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
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarTripulantesMision(?)}");
        cs.setInt(1, misionID);
        return cs.executeQuery();
    }

    public ResultSet listarMisionesTripulante(int tripulanteID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarMisionesTripulante(?)}");
        cs.setInt(1, tripulanteID);
        return cs.executeQuery();
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

    public void registrarCapacidad(int tripulanteID, int aptitudID, int calificacion, String fecha) {
        try {
            tripulanteLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Capacidades.json");
                List<CapacidadJson> capacidades = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<CapacidadJson>>() {});

                CapacidadJson nueva = new CapacidadJson();
                nueva.TripulanteID = tripulanteID;
                nueva.AptitudID = aptitudID;
                nueva.Calificacion = calificacion;
                nueva.FechaCapacidades = fecha;

                capacidades.add(nueva);
                escribirCapacidadesEnJsonSinLock(capacidades);
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

    public void registrarEvento(int misionID, String titulo, String desc, Timestamp fecha) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL AEvento(?, ?, ?, ?)}");
        cs.setInt(1, misionID);
        cs.setString(2, titulo);
        cs.setString(3, desc);
        cs.setTimestamp(4, fecha);
        cs.execute();
    }

    public void bajaEvento(int eventoID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL BEvento(?, ?)}");
        cs.setInt(1, eventoID);
        cs.setInt(2, 2);
        cs.execute();
    }

    public ResultSet listarEventos() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarEventos()}");
        return cs.executeQuery();
    }

    public ResultSet consultarEventos(int misionID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarEventos(?)}");
        cs.setInt(1, misionID);
        return cs.executeQuery();
    }

    public ResultSet verLogs() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL VerLogs()}");
        return cs.executeQuery();
    }
}
