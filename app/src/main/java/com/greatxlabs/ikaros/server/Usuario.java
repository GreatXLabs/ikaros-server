package com.greatxlabs.ikaros.server;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Usuario {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final SemaforoRW usuarioLock = new SemaforoRW();

    private int usuarioID;
    private int rolID;
    private int estadoUID;
    private String nombre;
    private String apellido;
    private String usuario;
    private String clave;

    public Usuario() {}

    @JsonProperty("UsuarioID")
    public int getUsuarioID() { return usuarioID; }
    public void setUsuarioID(int usuarioID) { this.usuarioID = usuarioID; }

    @JsonProperty("RolID")
    public int getRolID() { return rolID; }
    public void setRolID(int rolID) { this.rolID = rolID; }

    @JsonProperty("EstadoUID")
    public int getEstadoUID() { return estadoUID; }
    public void setEstadoUID(int estadoUID) { this.estadoUID = estadoUID; }

    @JsonProperty("Nombre")
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @JsonProperty("Apellido")
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    @JsonProperty("Usuario")
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    @JsonProperty("Clave")
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public boolean estaInactivo() {
        return estadoUID == 2;
    }

    public void desactivar() {
        this.estadoUID = 2;
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

    private static final String[] COLUMNAS_USUARIO = {
        "ID", "USUARIO", "NOMBRE", "APELLIDO", "CLAVE", "ROLNOMBRE", "ROLID", "ESTADONOMBRE"
    };

    public static List<Usuario> leerDesdeJson() {
        try {
            usuarioLock.iniciarLectura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                return mapper.readValue(ruta.toFile(), new TypeReference<List<Usuario>>() {});
            } finally {
                usuarioLock.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer Usuarios.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static void escribirEnJsonSinLock(List<Usuario> usuarios) throws IOException {
        Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
        Path tmp  = Files.createTempFile(ruta.getParent(), "Usuarios", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), usuarios);
        Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING);
    }

    public static List<RolJson> leerRolesDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "Roles.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<RolJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer Roles.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<EstadoJson> leerEstadosDesdeJson() {
        try {
            Path ruta = Path.of(Configuracion.getDataDir(), "EstadosUsuarios.json");
            return mapper.readValue(ruta.toFile(), new TypeReference<List<EstadoJson>>() {});
        } catch (Exception e) {
            System.err.println("Error al leer EstadosUsuarios.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static String obtenerNombreRolPorId(int rolId) {
        List<RolJson> roles = leerRolesDesdeJson();
        for (RolJson r : roles) {
            if (r.RolID == rolId) {
                return r.Rol;
            }
        }
        return null;
    }

    public static String obtenerNombreEstadoPorId(int estadoUid) {
        List<EstadoJson> estados = leerEstadosDesdeJson();
        for (EstadoJson e : estados) {
            if (e.EstadoUID == estadoUid) {
                return e.Estado;
            }
        }
        return null;
    }

    private static List<Map<String, Object>> obtenerUsuariosParaListar() {
        try {
            List<Usuario> usuarios = leerDesdeJson();

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

    public static boolean validarLogin(String usuario, String clave) {
        List<Usuario> usuarios = leerDesdeJson();
        for (Usuario u : usuarios) {
            if (u.getUsuario() != null && u.getUsuario().equals(usuario)
                    && u.getClave() != null && BCrypt.verifyer().verify(clave.toCharArray(), u.getClave()).verified
                    && u.getEstadoUID() == 1) {
                return true;
            }
        }
        return false;
    }

    public static UsuarioLoginResult obtenerDatosUsuarioParaLogin(String usuario) {
        List<Usuario> usuarios = leerDesdeJson();
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

    public static ResultSet consultarRoles() {
        List<RolJson> roles = leerRolesDesdeJson();
        List<String[]> filas = new ArrayList<>();
        for (RolJson r : roles) {
            filas.add(new String[]{String.valueOf(r.RolID), r.Rol});
        }
        return new AccesoDatos.SimpleResultSet(filas, 2);
    }

    public static int registrarUsuario(int rolID, String usuario, String nombre, String apellido, String clave) {
        String claveHash = BCrypt.withDefaults().hashToString(12, clave.toCharArray());
        try {
            usuarioLock.iniciarEscritura();
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
                escribirEnJsonSinLock(usuarios);
                return nuevoId;
            } finally {
                usuarioLock.terminarEscritura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al registrar usuario: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
        }
        return -1;
    }

    public static void modificarUsuario(int usuarioIDLogueado, int usuarioID, int rolID, String usuario, String nombre, String apellido, String clave) {
        String claveHash = (clave != null && !clave.isEmpty())
                ? BCrypt.withDefaults().hashToString(12, clave.toCharArray())
                : null;
        String desc = "";
        try {
            usuarioLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<Usuario> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Usuario>>() {});
                boolean encontrado = false;
                StringBuilder descBuilder = new StringBuilder();

                for (Usuario u : usuarios) {
                    if (u.getUsuarioID() == usuarioID) {
                        if (rolID != 0 && u.getRolID() != rolID) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Rol:").append(obtenerNombreRolPorId(u.getRolID())).append("->").append(obtenerNombreRolPorId(rolID));
                            u.setRolID(rolID);
                        }
                        if (usuario != null && !usuario.isEmpty() && !usuario.equals(u.getUsuario())) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Usuario:").append(u.getUsuario()).append("->").append(usuario);
                            u.setUsuario(usuario);
                        }
                        if (nombre != null && !nombre.isEmpty() && !nombre.equals(u.getNombre())) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Nombre:").append(u.getNombre()).append("->").append(nombre);
                            u.setNombre(nombre);
                        }
                        if (apellido != null && !apellido.isEmpty() && !apellido.equals(u.getApellido())) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Apellido:").append(u.getApellido()).append("->").append(apellido);
                            u.setApellido(apellido);
                        }
                        if (claveHash != null) {
                            if (descBuilder.length() > 0) descBuilder.append("|");
                            descBuilder.append("Clave: ***");
                            u.setClave(claveHash);
                        }
                        encontrado = true;
                        desc = descBuilder.toString();
                        break;
                    }
                }

                if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
                escribirEnJsonSinLock(usuarios);
            } finally {
                usuarioLock.terminarEscritura();
            }
            if (!desc.isEmpty()) {
                Registro.registrarLog(usuarioIDLogueado, 14, 4, usuarioID, desc);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al modificar usuario: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al modificar usuario: " + e.getMessage());
        }
    }

    public static void baja(int usuarioIDLogueado, int usuarioID) {
        boolean encontrado = false;
        String desc = "";
        try {
            usuarioLock.iniciarEscritura();
            try {
                Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                List<Usuario> usuarios = mapper.readValue(ruta.toFile(),
                        new TypeReference<List<Usuario>>() {});

                for (Usuario u : usuarios) {
                    if (u.getUsuarioID() == usuarioID) {
                        String estadoAnterior = obtenerNombreEstadoPorId(u.getEstadoUID());
                        u.desactivar();
                        String estadoActual = obtenerNombreEstadoPorId(u.getEstadoUID());
                        escribirEnJsonSinLock(usuarios);
                        desc = "Estado:" + estadoAnterior + "->" + estadoActual;
                        encontrado = true;
                        break;
                    }
                }

                if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
            } finally {
                usuarioLock.terminarEscritura();
            }
            if (encontrado) {
                Registro.registrarLog(usuarioIDLogueado, 17, 4, usuarioID, desc);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida al dar de baja usuario: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al dar de baja usuario: " + e.getMessage());
        }
    }

    public static void baja(int usuarioIDLogueado, String usuarioOID) {
        try {
            baja(usuarioIDLogueado, Integer.parseInt(usuarioOID));
        } catch (NumberFormatException e) {
            boolean encontrado = false;
            String desc = "";
            int targetID = 0;
            try {
                usuarioLock.iniciarEscritura();
                try {
                    Path ruta = Path.of(Configuracion.getDataDir(), "Usuarios.json");
                    List<Usuario> usuarios = mapper.readValue(ruta.toFile(),
                            new TypeReference<List<Usuario>>() {});
                    for (Usuario u : usuarios) {
                        if (u.getUsuario() != null && u.getUsuario().equals(usuarioOID)) {
                            String estadoAnterior = obtenerNombreEstadoPorId(u.getEstadoUID());
                            u.desactivar();
                            String estadoActual = obtenerNombreEstadoPorId(u.getEstadoUID());
                            escribirEnJsonSinLock(usuarios);
                            desc = "Estado:" + estadoAnterior + "->" + estadoActual;
                            targetID = u.getUsuarioID();
                            encontrado = true;
                            break;
                        }
                    }
                    if (!encontrado) throw new IllegalArgumentException("Usuario no encontrado: " + usuarioOID);
                } finally {
                    usuarioLock.terminarEscritura();
                }
                if (encontrado) {
                    Registro.registrarLog(usuarioIDLogueado, 17, 4, targetID, desc);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("Operación interrumpida al dar de baja usuario: " + ie.getMessage());
            } catch (IOException ioe) {
                System.err.println("Error al dar de baja usuario: " + ioe.getMessage());
            }
        }
    }

    public static ResultSet listar() throws SQLException {
        return new UsuarioResultSet(obtenerUsuariosParaListar());
    }

    public static int obtenerUsuarioID(String usuario) {
        List<Usuario> usuarios = leerDesdeJson();
        for (Usuario u : usuarios) {
            if (u.getUsuario() != null && u.getUsuario().equals(usuario) && u.getEstadoUID() == 1) {
                return u.getUsuarioID();
            }
        }
        throw new IllegalArgumentException("Usuario no encontrado: " + usuario);
    }

    public static boolean isUsuarioInactivo(int id) {
        List<Usuario> usuarios = leerDesdeJson();
        for (Usuario u : usuarios) {
            if (u.getUsuarioID() == id) return u.estaInactivo();
        }
        return false;
    }

    public static Map<String, Integer> obtenerRolesComoMapa() {
        Map<String, Integer> mapa = new HashMap<>();
        List<RolJson> roles = leerRolesDesdeJson();
        for (RolJson r : roles) {
            mapa.put(r.Rol.toUpperCase(), r.RolID);
        }
        return mapa;
    }

    static class UsuarioResultSet implements ResultSet {

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
}
