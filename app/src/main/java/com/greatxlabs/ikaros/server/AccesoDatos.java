package com.greatxlabs.ikaros.server;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private static final SemaforoRW registroLock = new SemaforoRW();

    private static void asegurarArchivo(String nombre) throws IOException {
        Path destino = Path.of(Configuracion.getDataDir(), nombre);
        if (Files.exists(destino)) {
            if (!"Usuarios.json".equals(nombre) || !tienePasswordsEnTextoPlano(destino)) return;
            System.out.println("Seed desactualizado detectado, sobrescribiendo: " + destino);
        }
        Files.createDirectories(destino.getParent());
        try (InputStream is = AccesoDatos.class.getClassLoader().getResourceAsStream(nombre)) {
            if (is == null) throw new IOException("Recurso semilla no encontrado: " + nombre);
            Files.copy(is, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("Archivo sembrado: " + destino);
    }

    private static boolean tienePasswordsEnTextoPlano(Path ruta) {
        try {
            List<Map<String, Object>> usuarios = mapper.readValue(ruta.toFile(), new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> u : usuarios) {
                Object clave = u.get("Clave");
                if (clave instanceof String s && (s.length() < 50 || !s.startsWith("$2"))) return true;
            }
        } catch (Exception e) {
            return true;
        }
        return false;
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

    private List<Usuario> leerUsuariosDesdeJson() {
        try {
            jsonLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<Usuario>>() {});
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

    private void escribirUsuariosEnJsonSinLock(List<Usuario> usuarios) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
        Path tmp  = Files.createTempFile(ruta.getParent(), "Usuarios", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), usuarios);
        Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING);
    }

    private List<RolJson> leerRolesDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Roles.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<RolJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Roles.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EstadoJson> leerEstadosDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "EstadosUsuarios.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EstadoJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer EstadosUsuarios.json: " + e.getMessage());
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

    private List<Registro> leerRegistrosDesdeJson() {
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

    private void escribirRegistrosEnJsonSinLock(List<Registro> registros) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Registros.json");
        Path tmp = Files.createTempFile(ruta.getParent(), "Registros", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), registros);
        Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING);
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

    private String obtenerNombreMisionPorId(int misionID) {
        return Mision.obtenerNombrePorId(misionID);
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

    static class SimpleResultSet implements ResultSet {

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
            List<Usuario> usuarios = leerUsuariosDesdeJson();

            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Usuario usuario : usuarios) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("ID", usuario.getUsuarioID());
                fila.put("USUARIO", usuario.getUsuario());
                fila.put("NOMBRE", usuario.getNombre());
                fila.put("APELLIDO", usuario.getApellido());
                fila.put("CLAVE", usuario.getClave());
                fila.put("ROLNOMBRE", obtenerNombreRolPorId(usuario.getRolID()));
                fila.put("ROLID", usuario.getRolID());
                fila.put("ESTADONOMBRE", obtenerNombreEstadoPorId(usuario.getEstadoUID()));
                resultado.add(fila);
            }
            return resultado;
        } catch (Exception e) {
            System.err.println("Error al obtener usuarios para listar: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean validarLogin(String usuario, String clave) {
        List<Usuario> usuarios = leerUsuariosDesdeJson();
        for (Usuario u : usuarios) {
            if (u.getUsuario() != null && u.getUsuario().equals(usuario)
                    && u.getClave() != null && BCrypt.verifyer().verify(clave.toCharArray(), u.getClave()).verified
                    && u.getEstadoUID() == 1) {
                return true;
            }
        }
        return false;
    }

    public UsuarioLoginResult obtenerDatosUsuarioParaLogin(String usuario) {
        List<Usuario> usuarios = leerUsuariosDesdeJson();
        for (Usuario u : usuarios) {
            if (u.getUsuario() != null && u.getUsuario().equals(usuario) && u.getEstadoUID() == 1) {
                UsuarioLoginResult res = new UsuarioLoginResult();
                res.usuarioID = u.getUsuarioID();
                res.rol = obtenerNombreRolPorId(u.getRolID());
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
        return Capacidad.consultarAptitudes();
    }

    public ResultSet listarEstadosMision() throws SQLException {
        return Mision.listarEstados();
    }

    public ResultSet listarEstadosTripulante() {
        return Tripulante.listarEstados();
    }

    public ResultSet listarEstadosEvento() throws SQLException {
        return Evento.listarEstados();
    }

    public void registrarLog(int usuarioID, int accionID, int tipoEntidadID, int entidadID, String descripcion) {
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
        List<Usuario> usuarios = leerUsuariosDesdeJson();
        for (Usuario u : usuarios) {
            if (u.getUsuario() != null && u.getUsuario().equals(usuario) && u.getEstadoUID() == 1) {
                return u.getUsuarioID();
            }
        }
        throw new IllegalArgumentException("Usuario no encontrado: " + usuario);
    }

    public int registrarUsuario(int rolID, String usuario, String nombre, String apellido, String clave) {
        String claveHash = BCrypt.withDefaults().hashToString(12, clave.toCharArray());
        try {
            jsonLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<Usuario> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Usuario>>() {});

                int nuevoId = 1;
                for (Usuario u : usuarios) {
                    if (u.getUsuarioID() >= nuevoId) nuevoId = u.getUsuarioID() + 1;
                }

                Usuario nuevoUsuario = new Usuario();
                nuevoUsuario.setUsuarioID(nuevoId);
                nuevoUsuario.setRolID(rolID);
                nuevoUsuario.setEstadoUID(1);
                nuevoUsuario.setNombre(nombre);
                nuevoUsuario.setApellido(apellido);
                nuevoUsuario.setUsuario(usuario);
                nuevoUsuario.setClave(claveHash);

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
        String claveHash = (clave != null && !clave.isEmpty())
                ? BCrypt.withDefaults().hashToString(12, clave.toCharArray())
                : null;
        try {
            jsonLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<Usuario> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Usuario>>() {});
                boolean encontrado = false;
                StringBuilder desc = new StringBuilder();

                for (Usuario u : usuarios) {
                    if (u.getUsuarioID() == usuarioID) {
                        if (rolID != 0 && u.getRolID() != rolID) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Rol:").append(obtenerNombreRolPorId(u.getRolID())).append("->").append(obtenerNombreRolPorId(rolID));
                            u.setRolID(rolID);
                        }
                        if (usuario != null && !usuario.isEmpty() && !usuario.equals(u.getUsuario())) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Usuario:").append(u.getUsuario()).append("->").append(usuario);
                            u.setUsuario(usuario);
                        }
                        if (nombre != null && !nombre.isEmpty() && !nombre.equals(u.getNombre())) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Nombre:").append(u.getNombre()).append("->").append(nombre);
                            u.setNombre(nombre);
                        }
                        if (apellido != null && !apellido.isEmpty() && !apellido.equals(u.getApellido())) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Apellido:").append(u.getApellido()).append("->").append(apellido);
                            u.setApellido(apellido);
                        }
                        if (claveHash != null) {
                            if (desc.length() > 0) desc.append("|");
                            desc.append("Clave: ***");
                            u.setClave(claveHash);
                        }
                        encontrado = true;
                        break;
                    }
                }

                if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
                escribirUsuariosEnJsonSinLock(usuarios);
                if (desc.length() > 0) {
                    registrarLog(usuarioIDLogueado, 14, 4, usuarioID, desc.toString());
                }
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

    public void bajaUsuario(int usuarioIDLogueado, int usuarioID) {
        try {
            jsonLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<Usuario> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Usuario>>() {});
                boolean encontrado = false;

                for (Usuario u : usuarios) {
                    if (u.getUsuarioID() == usuarioID) {
                        String estadoAnterior = obtenerNombreEstadoPorId(u.getEstadoUID());
                        u.desactivar();
                        String estadoActual = obtenerNombreEstadoPorId(u.getEstadoUID());
                        escribirUsuariosEnJsonSinLock(usuarios);
                        String desc = "Estado:" + estadoAnterior + "->" + estadoActual;
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
                    List<Usuario> usuarios = mapper.readValue(ruta.toFile(),
                            new TypeReference<List<Usuario>>() {});
                    boolean encontrado = false;
                    for (Usuario u : usuarios) {
                        if (u.getUsuario() != null && u.getUsuario().equals(usuarioOID)) {
                            String estadoAnterior = obtenerNombreEstadoPorId(u.getEstadoUID());
                            u.desactivar();
                            String estadoActual = obtenerNombreEstadoPorId(u.getEstadoUID());
                            escribirUsuariosEnJsonSinLock(usuarios);
                            String desc = "Estado:" + estadoAnterior + "->" + estadoActual;
                            registrarLog(usuarioIDLogueado, 14, 4, u.getUsuarioID(), desc);
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
        return Mision.registrar(estadoMID, nombre, descripcion, ini, fin);
    }

    public void modificarMision(int usuarioIDLogueado, int id, String nombre, String desc, Timestamp ini, Timestamp fin) {
        Mision.modificar(usuarioIDLogueado, id, nombre, desc, ini, fin, this);
    }

    public void actualizarEstadoMision(int usuarioIDLogueado, int id, int estadoID, Integer retrasoInicio, Integer retrasoFin) {
        Mision.actualizarEstado(usuarioIDLogueado, id, estadoID, retrasoInicio, retrasoFin, this);
    }

    public ResultSet listarMisiones() throws SQLException {
        return Mision.listar();
    }

    public ResultSet consultarMision(int id) throws SQLException {
        return Mision.consultar(id);
    }

    public boolean existeMision(int id) throws SQLException {
        return Mision.existe(id);
    }

    public ResultSet registrarTripulante(int estadoTID, int sexoID, int peso, int altura,
            String nombre, String apellido, String imagen, Date fechaNacimiento) {
        return Tripulante.registrar(estadoTID, sexoID, peso, altura, nombre, apellido, imagen, fechaNacimiento);
    }

    public void modificarTripulante(int usuarioIDLogueado, int tripulanteID, int estadoTID, int sexoID, int peso, int altura,
            String nombre, String apellido, String imagen, Date fechaNacimiento) {
        Tripulante.modificar(usuarioIDLogueado, tripulanteID, estadoTID, sexoID, peso, altura, nombre, apellido, imagen, fechaNacimiento, this);
    }

    public void bajaTripulante(int usuarioIDLogueado, int tripulanteID) {
        Tripulante.baja(usuarioIDLogueado, tripulanteID, this);
    }

    public void asignarTripulante(int usuarioIDLogueado, int tripID, int misID, Timestamp fecha) {
        Mision.asignarTripulante(usuarioIDLogueado, tripID, misID, fecha, this);
    }

    public ResultSet listarTripulantes() {
        return Tripulante.listar();
    }

    public ResultSet listarTripulantesMision(int misionID) throws SQLException {
        return Tripulante.listarMision(misionID);
    }

    public ResultSet listarMisionesTripulante(int tripulanteID) throws SQLException {
        return Mision.listarMisionesTripulante(tripulanteID);
    }

    public ResultSet consultarTripulante(int tripulanteID) {
        return Tripulante.consultar(tripulanteID);
    }

    public boolean existeTripulante(int id) {
        return Tripulante.existe(id);
    }

    public boolean isTripulanteRetirado(int id) {
        return Tripulante.estaRetirado(id);
    }

    public boolean isMisionTerminada(int id) {
        return Mision.estaTerminada(id);
    }

    public boolean isUsuarioInactivo(int id) {
        List<Usuario> usuarios = leerUsuariosDesdeJson();
        for (Usuario u : usuarios) {
            if (u.getUsuarioID() == id) return u.estaInactivo();
        }
        return false;
    }

    public ResultSet consultarCapacidades(int tripulanteID) {
        return Capacidad.consultar(tripulanteID);
    }

    public void registrarCapacidad(int usuarioIDLogueado, int tripulanteID, int aptitudID, int calificacion, String fecha) {
        Capacidad.registrar(usuarioIDLogueado, tripulanteID, aptitudID, calificacion, fecha, this);
    }

    public void eliminarCapacidades(int tripulanteID) {
        Capacidad.eliminar(tripulanteID);
    }

    public int registrarEvento(int usuarioIDLogueado, int misionID, String titulo, String desc, Timestamp fecha) {
        return Evento.registrar(usuarioIDLogueado, misionID, titulo, desc, fecha, this);
    }

    public void bajaEvento(int usuarioIDLogueado, int eventoID) {
        Evento.baja(usuarioIDLogueado, eventoID, this);
    }

    public ResultSet listarEventos() throws SQLException {
        return Evento.listar();
    }

    public ResultSet consultarEventos(int misionID) throws SQLException {
        return Evento.consultarPorMision(misionID);
    }

    public Map<String, Integer> obtenerRolesComoMapa() {
        Map<String, Integer> mapa = new HashMap<>();
        List<RolJson> roles = leerRolesDesdeJson();
        for (RolJson r : roles) {
            mapa.put(r.Rol.toUpperCase(), r.RolID);
        }
        return mapa;
    }

    public Map<String, Integer> obtenerAptitudesComoMapa() {
        return Capacidad.obtenerAptitudesComoMapa();
    }

    public Map<String, Integer> obtenerEstadosMisionComoMapa() {
        return Mision.obtenerEstadosComoMapa();
    }

    public Map<String, Integer> obtenerEstadosTripulanteComoMapa() {
        return Tripulante.obtenerEstadosComoMapa();
    }

    public Map<String, Integer> obtenerEstadosEventoComoMapa() {
        return Evento.obtenerEstadosComoMapa();
    }

    public ResultSet verLogs() {
        List<Registro> registros = leerRegistrosDesdeJson();
        List<Usuario> usuarios = leerUsuariosDesdeJson();
        List<RolJson> roles = leerRolesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (Registro r : registros) {
            String nombreUsuario = "";
            String nombreRol = "";
            for (Usuario u : usuarios) {
                if (u.getUsuarioID() == r.getUsuarioID()) {
                    nombreUsuario = u.getUsuario() != null ? u.getUsuario() : "";
                    for (RolJson rol : roles) {
                        if (rol.RolID == u.getRolID()) {
                            nombreRol = rol.Rol;
                            break;
                        }
                    }
                    break;
                }
            }
            String nombreAccion = obtenerNombreAccionPorId(r.getAccionMID());
            String nombreEntidad = obtenerNombreEntidadPorId(r.getTipoEntidadID());
            filas.add(new String[]{
                String.valueOf(r.getRegistroID()),
                nombreUsuario,
                nombreRol,
                nombreAccion != null ? nombreAccion : "",
                nombreEntidad != null ? nombreEntidad : "",
                String.valueOf(r.getEntidadID()),
                r.getFechaHora() != null ? r.getFechaHora() : "",
                r.getDescripcion() != null ? r.getDescripcion() : ""
            });
        }
        filas.sort((a, b) -> b[6].compareTo(a[6]));
        return new SimpleResultSet(filas, 8);
    }
}
