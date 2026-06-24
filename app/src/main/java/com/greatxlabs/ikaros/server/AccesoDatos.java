package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    private static final ObjectMapper mapper = new ObjectMapper(); //CAMBIADO: agregado para leer JSON

    // Semaforo para acceso concurrent a los archivos JSON (solo lectura)
    private static final SemaforoRW jsonLock = new SemaforoRW(); //CAMBIADO: uso de SemaforoRW para concurrencia

    // Clase interna para mapear el JSON de usuarios
    private static class UsuarioJson {
        public int UsuarioID;
        public int RolID;
        public int EstadoUID;
        public String Nombre;
        public String Apellido;
        public String Usuario;
        public String Clave;

        // Constructor vacío requerido por Jackson
        public UsuarioJson() {}
    }

    // Clase interna para mapear el JSON de roles
    private static class RolJson {
        public int RolID;
        public String Rol;

        public RolJson() {}
    }

    // Clase interna para devolver los datos necesarios del login
    public static class UsuarioLoginResult {
        public int usuarioID;
        public String rol; // Nombre del rol (ej: "Jefe")
        public UsuarioLoginResult() {}
    }

    // Clase interna para mapear el JSON de estados de usuario
    private static class EstadoJson { //CAMBIADO: agregado para leer JSON de estados
        public int EstadoUID;
        public String Estado;

        public EstadoJson() {}
    }

    /**
     * Lee todos los usuarios desde el archivo JSON ubicado en classpath.
     * Se protege con un SemaforoRW en modo lectura para permitir concurrent reads.
     *
     * @return Lista de objetos UsuarioJson.
     */
    private List<UsuarioJson> leerUsuariosDesdeJson() { //CAMBIADO: nuevo método
        try {
            jsonLock.iniciarLectura(); //CAMBIADO: bloqueo de lectura
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("Usuarios.json");
                if (is == null) {
                    System.err.println("No se encontró el archivo Usuarios.json en el classpath.");
                    return Collections.emptyList();
                }
                return mapper.readValue(is, new TypeReference<List<UsuarioJson>>() {});
            } finally {
                jsonLock.terminarLectura(); //CAMBIADO: liberación de lectura
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer el archivo JSON de usuarios: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Escribe la lista de usuarios al archivo JSON ubicado en classpath.
     * Se protege con un SemaforoRW en modo escritura para acceso exclusivo.
     *
     * @param usuarios Lista de objetos UsuarioJson a guardar
     */
    private void escribirUsuariosEnJson(List<UsuarioJson> usuarios) { //CAMBIADO: nuevo método
        try {
            jsonLock.iniciarEscritura(); //CAMBIADO: bloqueo de escritura
            try {
                ObjectMapper writer = new ObjectMapper();
                writer.enable(SerializationFeature.INDENT_OUTPUT);
                File file = new File("src/main/resources/Usuarios.json");
                writer.writeValue(file, usuarios);
            } finally {
                jsonLock.terminarEscritura(); //CAMBIADO: liberación de escritura
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo JSON de usuarios: " + e.getMessage());
        }
    }
     /**
     * Lee todos los roles desde el archivo JSON ubicado en classpath.
     * Se protege con un SemaforoRW en modo lectura.
     *
     * @return Lista de objetos RolJson.
     */
    private List<RolJson> leerRolesDesdeJson() { //CAMBIADO: nuevo método
        try {
            jsonLock.iniciarLectura(); //CAMBIADO: bloqueo de lectura
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("Roles.json");
                if (is == null) {
                    System.err.println("No se encontró el archivo Roles.json en el classpath.");
                    return Collections.emptyList();
                }
                return mapper.readValue(is, new TypeReference<List<RolJson>>() {});
            } finally {
                jsonLock.terminarLectura(); //CAMBIADO: liberación de lectura
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer el archivo JSON de roles: " + e.getMessage());
            return Collections.emptyList();
        }
    }

     /**
     * Lee todos los estados de usuario desde el archivo JSON ubicado en classpath.
     * Se protege con un SemaforoRW en modo lectura.
     *
     * @return Lista de objetos EstadoJson.
     */
    private List<EstadoJson> leerEstadosDesdeJson() { //CAMBIADO: nuevo método
        try {
            jsonLock.iniciarLectura(); //CAMBIADO: bloqueo de lectura
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("EstadosUsuarios.json");
                if (is == null) {
                    System.err.println("No se encontró el archivo EstadosUsuarios.json en el classpath.");
                    return Collections.emptyList();
                }
                return mapper.readValue(is, new TypeReference<List<EstadoJson>>() {});
            } finally {
                jsonLock.terminarLectura(); //CAMBIADO: liberación de lectura
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al leer el archivo JSON de estados de usuario: " + e.getMessage());
            return Collections.emptyList();
        }
        }
    }
    }

    /**
     * Obtiene el nombre del rol a partir de su ID, leyendo el archivo Roles.json.
     * El método internamente usa {@code leerRolesDesdeJson} que ya está protegido por SemaforoRW.
     *
     * @param rolId ID del rol.
     * @return Nombre del rol (ej: "Jefe") o null si no se encuentra.
     */
    private String obtenerNombreRolPorId(int rolId) { //CAMBIADO: nuevo método
        List<RolJson> roles = leerRolesDesdeJson();
        for (RolJson r : roles) {
            if (r.RolID == rolId) {
                return r.Rol;
            }
        }
        return null;
    }

    /**
     * Obtiene el nombre del estado de usuario a partir de su ID, leyendo el archivo EstadosUsuarios.json.
     * El método internamente usa {@code leerEstadosDesdeJson} que ya está protegido por SemaforoRW.
     *
     * @param estadoUid ID del estado de usuario.
     * @return Nombre del estado (ej: "Activo") o null si no se encuentra.
     */
    private String obtenerNombreEstadoPorId(int estadoUid) { //CAMBIADO: nuevo método
        List<EstadoJson> estados = leerEstadosDesdeJson();
        for (EstadoJson e : estados) {
            if (e.EstadoUID == estadoUid) {
                return e.Estado;
            }
        }
        return null;
    }

    // Clase interna para envolver la lista de usuarios como ResultSet (para compatibilidad)
    private class UsuarioResultSet implements ResultSet { //CAMBIADO: clase adaptadora para JSON
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
        public Object getObject(String columnLabel) throws SQLException {
            if (indiceActual < 0 || indiceActual >= filas.size()) {
                throw new SQLException("No hay fila actual");
            }
            Map<String, Object> fila = filas.get(indiceActual);
            return fila.get(columnLabel.toUpperCase()); // Los nombres de columna en mayúsculas
        }

        @Override
        public String getString(String columnLabel) throws SQLException {
            Object valor = getObject(columnLabel);
            return valor == null ? null : valor.toString();
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            Object valor = getObject(columnLabel);
            return valor == null ? 0 : Integer.parseInt(valor.toString());
        }

        @Override
        public Timestamp getTimestamp(String columnLabel) throws SQLException {
            Object valor = getObject(columnLabel);
            return valor == null ? null : Timestamp.valueOf(valor.toString());
        }

        @Override
        public Date getDate(String columnLabel) throws SQLException {
            Object valor = getObject(columnLabel);
            return valor == null ? null : Date.valueOf(valor.toString());
        }

        // Métodos adicionales necesarios para ResultSet (implementaciones mínimas)
        @Override public void close() throws SQLException {}
        @Override public boolean wasNull() throws SQLException { return false; }
        @Override public boolean getBoolean(String columnLabel) throws SQLException { return false; }
        @Override public byte getByte(String columnLabel) throws SQLException { return 0; }
        @Override public short getShort(String columnLabel) throws SQLException { return 0; }
        @Override public long getLong(String columnLabel) throws SQLException { return 0; }
        @Override public float getFloat(String columnLabel) throws SQLException { return 0; }
        @Override public double getDouble(String columnLabel) throws SQLException { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException { return null; }
        @Override public java.math.BigDecimal getBigDecimal(String columnLabel) throws SQLException { return null; }
        @Override public InputStream getAsciiStream(String columnLabel) throws SQLException { return null; }
        @Override public InputStream getUnicodeStream(String columnLabel) throws SQLException { return null; }
        @Override public InputStream getBinaryStream(String columnLabel) throws SQLException { return null; }
        @Override public String getString(String columnLabel, int scale) throws SQLException { return getString(columnLabel); }
        }
        }
        @Override public boolean getterStillValid() { return true; }
        @Override public boolean next() throws SQLException { return false; }
        @Override public void close() throws SQLException { }
        @Override public boolean wasNull() throws SQLException { return false; }
        @Override public String getString(int columnIndex) throws SQLException { return null; }
        @Override public boolean getBoolean(int columnIndex) throws SQLException { return false; }
        @Override public byte getByte(int columnIndex) throws SQLException { return 0; }
        @Override public short getShort(int columnIndex) throws SQLException { return 0; }
        @Override public int getInt(int columnIndex) throws SQLException { return 0; }
        @Override public long getLong(int columnIndex) throws SQLException { return 0; }
        @Override public float getFloat(int columnIndex) throws SQLException { return 0; }
        @Override public double getDouble(int columnIndex) throws SQLException { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException { return null; }
        @Override public java.math.BigDecimal getBigDecimal(int columnIndex) throws SQLException { return null; }
        @Override public InputStream getAsciiStream(int columnIndex) throws SQLException { return null; }
        @Override public InputStream getUnicodeStream(int columnIndex) throws SQLException { return null; }
        @Override public InputStream getBinaryStream(int columnIndex) throws SQLException { return null; }
        @Override public SQLWarning getWarnings() { return null; }
        @Override public void clearWarnings() throws SQLException { }
        @Override public String getCursorName() throws SQLException { return null; }
        @Override public boolean getMoreResults() throws SQLException { return false; }
        @Override public boolean getMoreResults(int current) throws SQLException { return false; }
        @Override public boolean getMoreResults(int current) throws SQLException { return false; }
        @Override public ResultSet getGeneratedKeys() throws SQLException { return null; }
        @Override public boolean execute(String sql) throws SQLException { return false; }
        @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { return false; }
        @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException { return false; }
        @Override public boolean execute(String sql, String[] columnNames) throws SQLException { return false; }
        @Override public boolean executeUpdate(String sql) throws SQLException { return 0; }
        @Override public boolean executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { return 0; }
        @Override public boolean executeUpdate(String sql, int[] columnIndexes) throws SQLException { return 0; }
        @Override public boolean executeUpdate(String sql, String[] columnNames) throws SQLException { return 0; }
        @Override public boolean closeOnCompletion() throws SQLException { return false; }
        @Override public void enableCloseOnCompletion() throws SQLException { }
        @Override public boolean isCloseOnCompletion() throws SQLException { return false; }
        @Override public String getString(int columnIndex) throws SQLException { return null;        }
        @Override public boolean getBoolean(int columnIndex) throws SQLException { return false; }
        @Override public byte getByte(int columnIndex) throws SQLException { return 0; }
        @Override public short getShort(int columnIndex) throws SQLException { return 0; }
        @Override public int getInt(int columnIndex) throws SQLException { return 0; }
        @Override public long getLong(int columnIndex) throws SQLException { return 0; }
        @Override public float getFloat(int columnIndex) throws SQLException { return 0; }
        @Override public double getDouble(int columnIndex) throws SQLException { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException { return null; }
        @Override public java.math.BigDecimal getBigDecimal(int columnIndex) throws SQLException { return null; }
        @Override public InputStream getAsciiStream(int columnIndex) throws SQLException { return null; }
        @Override public InputStream getUnicodeStream(int columnIndex) throws SQLException { return null; }
        @Override public InputStream getBinaryStream(int columnIndex) throws SQLException { return null; }
        @Override public Reader getCharacterStream(int columnIndex) throws SQLException { return null; }
        @Override public Reader getCharacterStream(int columnIndex) throws SQLException { return null; }
        @Override public Array getArray(int columnIndex) throws SQLException { return null; }
        @Override public Array getArray(String columnLabel) throws SQLException { return null; }
        @Override public Blob getBlob(int columnIndex) throws SQLException { return null; }
        @Override public Blob getBlob(String columnLabel) throws SQLException { return null; }
        @Override public Clob getClob(int columnIndex) throws SQLException { return null; }
        @Override public Clob getClob(String columnLabel) throws SQLException { return null; }
        @Override public Ref getRef(int columnIndex) throws SQLException { return null; }
        @Override public Ref getRef(String columnLabel) throws SQLException { return null; }
        @Override public RowId getRowId(int columnIndex) throws SQLException { return null; }
        @Override public RowId getRowId(String columnLabel) throws SQLException { return null; }
        @Override public NClob getNClob(int columnIndex) throws SQLException { return null; }
        @Override public NClob getNClob(String columnLabel) throws SQLException { return null; }
        @Override public SQLXML getSQLXML(int columnIndex) throws SQLException { return null; }
        @Override public SQLXML getSQLXML(String columnLabel) throws SQLException { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override public void close() throws SQLException { }
    }

    /**
     * Obtiene los usuarios listados combinando datos de Usuarios.json, Roles.json y EstadosUsuarios.json.
     * Se protege con SemaforoRW en modo lectura para permitir concurrent reads.
     *
     * @return Lista de mapas donde cada mapa representa una fila con las columnas esperadas por el ResultSet original.
     */
    private List<Map<String, Object>> obtenerUsuariosParaListar() { //CAMBIADO: nuevo método
        try {
            jsonLock.iniciarLectura(); //CAMBIADO: bloqueo de lectura
            try {
                List<UsuarioJson> usuarios = leerUsuariosDesdeJson();
                List<RolJson> roles = leerRolesDesdeJson();
                List<EstadoJson> estados = leerEstadosDesdeJson();

                List<Map<String, Object>> resultado = new ArrayList<>();

                for (UsuarioJson usuario : usuarios) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("USUARIOID", usuario.UsuarioID);
                    fila.put("ROLID", usuario.RolID);
                    fila.put("NOMBRE", usuario.Nombre);
                    fila.put("APELLIDO", usuario.Apellido);
                    fila.put("USUARIO", usuario.Usuario);
                    fila.put("CLAVE", usuario.Clave);
                    fila.put("NOMBREROL", obtenerNombreRolPorId(usuario.RolID));
                    fila.put("ESTADO", obtenerNombreEstadoPorId(usuario.EstadoUID));
                    fila.put("ESTADOUID", usuario.EstadoUID);
                    resultado.add(fila);
                }

                return resultado;
            } finally {
                jsonLock.terminarLectura(); //CAMBIADO: liberación de lectura
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error al obtener usuarios para listar: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- AUTENTICACIÓN Y SESIÓN ---
    public boolean validarLogin(String usuario, String clave) { //CAMBIADO: ahora usa JSON en lugar de BD
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson(); // lectura protegida por SemaforoRW
        for (UsuarioJson u : usuarios) {
            if (u.Usuario != null && u.Usuario.equals(usuario)
                    && u.Clave != null && u.Clave.equals(clave)
                    && u.EstadoUID == 1) { // 1 = Activo
                return true;
            }
        }
        return false;
    }

    /**
     * Obtiene los datos necesarios para iniciar sesión (usuarioID y rol) desde el JSON.
     *
     * @param usuario Nombre de usuario.
     * @return Objeto con usuarioID y rol, o null si no se encuentra o no está activo.
     */
    public UsuarioLoginResult obtenerDatosUsuarioParaLogin(String usuario) { //CAMBIADO: nuevo método
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson(); // lectura protegida por SemaforoRW
        for (UsuarioJson u : usuarios) {
            if (u.Usuario != null && u.Usuario.equals(usuario) && u.EstadoUID == 1) {
                UsuarioLoginResult res = new UsuarioLoginResult();
                res.usuarioID = u.UsuarioID;
                // Obtener nombre del rol desde Roles.json
                res.rol = obtenerNombreRolPorId(u.RolID);
                return res;
            }
        }
        return null;
    }

    // Mantener el método original para compatibilidad (aunque no se usa en login)
    public ResultSet obtenerDatosUsuario(String usuario) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarUsuario(?)}");
        cs.setString(1, usuario);
        return cs.executeQuery();
    }


    // --- ROLES ---
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

    // --- REGISTROS (LOGS) ---
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

    public int obtenerUsuarioID(String usuario) { //CAMBIADO: ahora usa JSON en lugar de BD
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson(); // lectura protegida por SemaforoRW
        for (UsuarioJson u : usuarios) {
            if (u.Usuario != null && u.Usuario.equals(usuario) && u.EstadoUID == 1) { // 1 = Activo
                return u.UsuarioID;
            }
        }
        throw new IllegalArgumentException("Usuario no encontrado: " + usuario);
    }

    // --- USUARIOS ---
    public void registrarUsuario(int rolID, String usuario, String nombre, String apellido, String clave) { //CAMBIADO: ahora usa JSON en lugar de BD
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson(); // lectura protegida por SemaforoRW

        // Generar un nuevo ID (el máximo actual + 1)
        int nuevoId = 1;
        for (UsuarioJson u : usuarios) {
            if (u.UsuarioID >= nuevoId) {
                nuevoId = u.UsuarioID + 1;
            }
        }

        // Crear el nuevo usuario
        UsuarioJson nuevoUsuario = new UsuarioJson();
        nuevoUsuario.UsuarioID = nuevoId;
        nuevoUsuario.RolID = rolID;
        nuevoUsuario.EstadoUID = 1; // 1 = Activo por defecto
        nuevoUsuario.Nombre = nombre;
        nuevoUsuario.Apellido = apellido;
        nuevoUsuario.Usuario = usuario;
        nuevoUsuario.Clave = clave;

        usuarios.add(nuevoUsuario);
        escribirUsuariosEnJson(usuarios); // escritura protegida por SemaforoRW
    }

    public void modificarUsuario(int usuarioID, int rolID, String usuario, String nombre, String apellido, String clave) { //CAMBIADO: ahora usa JSON en lugar de BD
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson(); // lectura protegida por SemaforoRW
        boolean usuarioEncontrado = false;

        for (UsuarioJson u : usuarios) {
            if (u.UsuarioID == usuarioID) {
                // Actualizar los campos proporcionados (pero solo si no son null o vacíos)
                if (rolID != 0) { // Asumiendo que 0 significa "no cambiar"
                    u.RolID = rolID;
                }
                if (usuario != null && !usuario.isEmpty()) {
                    u.Usuario = usuario;
                }
                if (nombre != null && !nombre.isEmpty()) {
                    u.Nombre = nombre;
                }
                if (apellido != null && !apellido.isEmpty()) {
                    u.Apellido = apellido;
                }
                if (clave != null && !clave.isEmpty()) {
                    u.Clave = clave;
                }
                usuarioEncontrado = true;
                break;
            }
        }

        if (usuarioEncontrado) {
            escribirUsuariosEnJson(usuarios); // escritura protegida por SemaforoRW
        } else {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioID);
        }
    }

    // TODO: obtenerClaveUsuario expone la contrasena en texto plano — ver issue #15
    // Se elimina cuando usuarios se migre a archivos (el hashing se implementa ahi)
    public String obtenerClaveUsuario(String usuario) { //CAMBIADO: ahora usa JSON en lugar de BD
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson(); // lectura protegida por SemaforoRW
        for (UsuarioJson u : usuarios) {
            if (u.Usuario != null && u.Usuario.equals(usuario) && u.EstadoUID == 1) { // 1 = Activo
                return u.Clave;
            }
        }
        return "";
    }

    public void bajaUsuario(String nombreUsuario) { //CAMBIADO: ahora usa JSON en lugar de BD
        List<UsuarioJson> usuarios = leerUsuariosDesdeJson(); // lectura protegida por SemaforoRW
        boolean usuarioEliminado = false;

        Iterator<UsuarioJson> iterator = usuarios.iterator();
        while (iterator.hasNext()) {
            UsuarioJson u = iterator.next();
            if (u.Usuario != null && u.Usuario.equals(nombreUsuario)) {
                iterator.remove();
                usuarioEliminado = true;
                break;
            }
        }

        if (usuarioEliminado) {
            escribirUsuariosEnJson(usuarios); // escritura protegida por SemaforoRW
        } else {
            throw new IllegalArgumentException("Usuario no encontrado: " + nombreUsuario);
        }
    }

    public ResultSet listarUsuarios() throws SQLException {
        return new UsuarioResultSet(obtenerUsuariosParaListar()); //CAMBIADO: ahora usa JSON en lugar de BD
    }

    /**
     * Escribe la lista completa de usuarios al archivo JSON ubicado en classpath.
     * Se protege con un SemaforoRW en modo escritura para acceso exclusivo.
     *
     * @param usuarios Lista de objetos UsuarioJson a guardar
     */
    private void escribirUsuariosEnJson(List<UsuarioJson> usuarios) { //CAMBIADO: nuevo método
        try {
            jsonLock.iniciarEscritura(); //CAMBIADO: bloqueo de escritura
            try {
                File archivo = new File("src/main/resources/Usuarios.json");
                mapper.writerWithDefaultPrettyPrinter().writeValue(archivo, usuarios);
            } finally {
                jsonLock.terminarEscritura(); //CAMBIADO: liberación de escritura
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación de escritura interrumpida: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo JSON de usuarios: " + e.getMessage());
        }
    }

    // --- MISIONES ---
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

    // --- TRIPULANTES ---
    public ResultSet registrarTripulante(int estadoTID, int sexoID, int peso, int altura, String nombre, String apellido, String imagen, Date fechaNacimiento) throws SQLException {
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

    public void modificarTripulante(int tripulanteID, int estadoTID, int sexoID, int peso, int altura, String nombre, String apellido, String imagen, Date fechaNacimiento) throws SQLException {
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

    // --- EVENTOS Y LOGS ---
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