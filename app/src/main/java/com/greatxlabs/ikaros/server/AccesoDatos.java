package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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

/**
 * Capa de acceso a datos. Todas las operaciones van por stored procedures.
 *
 * Patron de nombres de SP:
 *   A = Alta (registrar)   — ej: AUsuario, AMision, AEvento
 *   M = Modificacion       — ej: MUsuario, MMision
 *   B = Baja (logica)      — ej: BUsuario, BEvento
 *   + Consultas/Listas     — ej: ConsultarUsuario, ListarMisiones
 *
 * Todos los metodos obtienen conexion via ConexionBD.getConexion().
 * Los metodos que devuelven ResultSet no cierran el CallableStatement
 * internamente — queda a cargo del caller al cerrar el ResultSet.
 */
public class AccesoDatos {

    // Jackson ObjectMapper for JSON processing
    private static final ObjectMapper mapper = new ObjectMapper();

    // Semaforo para acceso concurrente a los archivos JSON
    private static final SemaforoRW jsonLock = new SemaforoRW();

    // -------------------------------------------------------------------------
    // Clases internas de mapeo JSON
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Lectura / escritura de archivos JSON
    // -------------------------------------------------------------------------

    private List<UsuarioJson> leerUsuariosDesdeJson() {
        try {
            jsonLock.iniciarLectura();
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("Usuarios.json");
                if (is == null) {
                    System.err.println("No se encontró el archivo Usuarios.json en el classpath.");
                    return Collections.emptyList();
                }
                return mapper.readValue(is, new TypeReference<List<UsuarioJson>>() {});
            } finally {
                jsonLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer el archivo JSON de usuarios: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void escribirUsuariosEnJson(List<UsuarioJson> usuarios) {
        try {
            jsonLock.iniciarEscritura();
            try {
                File archivo = new File("src/main/resources/Usuarios.json");
                mapper.writerWithDefaultPrettyPrinter().writeValue(archivo, usuarios);
            } finally {
                jsonLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación de escritura interrumpida: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo JSON de usuarios: " + e.getMessage());
        }
    }

    private List<RolJson> leerRolesDesdeJson() {
        try {
            jsonLock.iniciarLectura();
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("Roles.json");
                if (is == null) {
                    System.err.println("No se encontró el archivo Roles.json en el classpath.");
                    return Collections.emptyList();
                }
                return mapper.readValue(is, new TypeReference<List<RolJson>>() {});
            } finally {
                jsonLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer el archivo JSON de roles: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EstadoJson> leerEstadosDesdeJson() {
        try {
            jsonLock.iniciarLectura();
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("EstadosUsuarios.json");
                if (is == null) {
                    System.err.println("No se encontró el archivo EstadosUsuarios.json en el classpath.");
                    return Collections.emptyList();
                }
                return mapper.readValue(is, new TypeReference<List<EstadoJson>>() {});
            } finally {
                jsonLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer el archivo JSON de estados de usuario: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de lookup
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Adaptador ResultSet sobre la lista de usuarios del JSON
    // -------------------------------------------------------------------------

    /**
     * ResultSet mínimo que expone las filas construidas desde los archivos JSON.
     * Solo se implementan los métodos que el resto del código usa realmente;
     * el resto lanza UnsupportedOperationException para detectar usos inesperados.
     */
    // Orden de columnas que expone UsuarioResultSet (alineado con parseUsuarios del cliente):
    // 1=ID, 2=USUARIO, 3=NOMBRE, 4=APELLIDO, 5=CLAVE, 6=ROLNOMBRE, 7=ROLID, 8=ESTADONOMBRE
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

        // --- Métodos por índice ---
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

        // --- Métodos por nombre (resto de la interfaz) ---
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

        // --- Navegación y metadatos ---
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

    // -------------------------------------------------------------------------
    // Construcción de la lista de usuarios para listar
    // -------------------------------------------------------------------------

    private List<Map<String, Object>> obtenerUsuariosParaListar() {
        try {
            List<UsuarioJson> usuarios = leerUsuariosDesdeJson();

            List<Map<String, Object>> resultado = new ArrayList<>();
            for (UsuarioJson usuario : usuarios) {
                Map<String, Object> fila = new HashMap<>();
                // Orden de columnas alineado con COLUMNAS_USUARIO:
                // ID, USUARIO, NOMBRE, APELLIDO, CLAVE, ROLNOMBRE, ROLID, ESTADONOMBRE
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

    // -------------------------------------------------------------------------
    // AUTENTICACIÓN Y SESIÓN
    // -------------------------------------------------------------------------

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

    /** Mantener para compatibilidad con código que aún llama al SP. */
    public ResultSet obtenerDatosUsuario(String usuario) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarUsuario(?)}");
        cs.setString(1, usuario);
        return cs.executeQuery();
    }

    // -------------------------------------------------------------------------
    // ROLES / CATÁLOGOS
    // -------------------------------------------------------------------------

    public ResultSet consultarRoles() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarRoles()}");
        return cs.executeQuery();
    }

    public ResultSet consultarAptitudes() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarAptitudes()}");
        return cs.executeQuery();
    }

    public ResultSet listarEstadosMision() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarEstadosMisiones()}");
        return cs.executeQuery();
    }

    public ResultSet listarEstadosTripulante() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarEstadosTripulantes()}");
        return cs.executeQuery();
    }

    public ResultSet listarEstadosEvento() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarEstadosEventos()}");
        return cs.executeQuery();
    }

    // -------------------------------------------------------------------------
    // REGISTROS (LOGS)
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // USUARIOS
    // -------------------------------------------------------------------------

    public void registrarUsuario(int rolID, String usuario, String nombre, String apellido, String clave) {
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson();

        int nuevoId = 1;
        for (UsuarioJson u : usuarios) {
            if (u.UsuarioID >= nuevoId) {
                nuevoId = u.UsuarioID + 1;
            }
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
        escribirUsuariosEnJson(usuarios);
    }

    public void modificarUsuario(int usuarioID, int rolID, String usuario, String nombre, String apellido, String clave) {
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson();
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

        if (encontrado) {
            escribirUsuariosEnJson(usuarios);
        } else {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
        }
    }

    // TODO: obtenerClaveUsuario expone la contraseña en texto plano — ver issue #15
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
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson();
        boolean encontrado = false;

        for (UsuarioJson u : usuarios) {
            if (u.UsuarioID == usuarioID) {
                u.EstadoUID = 2; // baja lógica → Inactivo
                encontrado = true;
                break;
            }
        }

        if (encontrado) {
            escribirUsuariosEnJson(usuarios);
        } else {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
        }
    }

    /** Sobrecarga por nombre para compatibilidad. Si el argumento es numérico delega al método por ID. */
    public void bajaUsuario(String usuarioOID) {
        try {
            bajaUsuario(Integer.parseInt(usuarioOID));
        } catch (NumberFormatException e) {
            // Búsqueda por nombre de usuario (flujo legacy)
            List<UsuarioJson> usuarios = leerUsuariosDesdeJson();
            boolean encontrado = false;
            for (UsuarioJson u : usuarios) {
                if (u.Usuario != null && u.Usuario.equals(usuarioOID)) {
                    u.EstadoUID = 2;
                    encontrado = true;
                    break;
                }
            }
            if (encontrado) {
                escribirUsuariosEnJson(usuarios);
            } else {
                throw new IllegalArgumentException("Usuario no encontrado: " + usuarioOID);
            }
        }
    }

    public ResultSet listarUsuarios() throws SQLException {
        return new UsuarioResultSet(obtenerUsuariosParaListar());
    }

    // -------------------------------------------------------------------------
    // MISIONES
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // TRIPULANTES
    // -------------------------------------------------------------------------

    public ResultSet registrarTripulante(int estadoTID, int sexoID, int peso, int altura,
            String nombre, String apellido, String imagen, Date fechaNacimiento) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ATripulante(?, ?, ?, ?, ?, ?, ?, ?)}");
        cs.setInt(1, estadoTID);
        cs.setInt(2, sexoID);
        cs.setInt(3, peso);
        cs.setInt(4, altura);
        cs.setString(5, nombre);
        cs.setString(6, apellido);
        cs.setString(7, imagen);
        cs.setDate(8, new java.sql.Date(fechaNacimiento.getTime()));
        return cs.executeQuery();
    }

    public void modificarTripulante(int tripulanteID, int estadoTID, int sexoID, int peso, int altura,
            String nombre, String apellido, String imagen, Date fechaNacimiento) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL MTripulante(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
        cs.setInt(1, tripulanteID);
        cs.setInt(2, estadoTID);
        cs.setInt(3, sexoID);
        cs.setInt(4, peso);
        cs.setInt(5, altura);
        cs.setString(6, nombre);
        cs.setString(7, apellido);
        cs.setString(8, imagen);
        cs.setDate(9, new java.sql.Date(fechaNacimiento.getTime()));
        cs.execute();
    }

    public void bajaTripulante(int tripulanteID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL BTripulante(?)}");
        cs.setInt(1, tripulanteID);
        cs.execute();
    }

    public void asignarTripulante(int tripID, int misID, Timestamp fecha) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL AGrupoMision(?, ?, ?)}");
        cs.setInt(1, tripID);
        cs.setInt(2, misID);
        cs.setTimestamp(3, fecha);
        cs.execute();
    }

    public ResultSet listarTripulantes() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarTripulantes()}");
        return cs.executeQuery();
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

    public ResultSet consultarTripulante(int tripulanteID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarTripulante(?)}");
        cs.setInt(1, tripulanteID);
        return cs.executeQuery();
    }

    public boolean existeTripulante(int id) throws SQLException {
        return consultarTripulante(id).next();
    }

    public ResultSet consultarCapacidades(int tripulanteID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarCapacidades(?)}");
        cs.setInt(1, tripulanteID);
        return cs.executeQuery();
    }

    public void registrarCapacidad(int tripulanteID, int aptitudID, int calificacion, String fecha) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL RegistrarCapacidad(?, ?, ?, ?)}");
        cs.setInt(1, tripulanteID);
        cs.setInt(2, aptitudID);
        cs.setInt(3, calificacion);
        cs.setString(4, fecha);
        cs.execute();
    }

    public void eliminarCapacidades(int tripulanteID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL EliminarCapacidades(?)}");
        cs.setInt(1, tripulanteID);
        cs.execute();
    }

    // -------------------------------------------------------------------------
    // EVENTOS Y LOGS
    // -------------------------------------------------------------------------

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
