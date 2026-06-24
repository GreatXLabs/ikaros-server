package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Collections;
import java.util.List;

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

    public int obtenerUsuarioID(String usuario) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarUsuario(?)}");
        cs.setString(1, usuario);
        ResultSet rs = cs.executeQuery();
        if (rs.next()) return rs.getInt("UsuarioID");
        throw new SQLException("Usuario no encontrado: " + usuario);
    }

    // --- USUARIOS ---
    public void registrarUsuario(int rolID, String usuario, String nombre, String apellido, String clave) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL AUsuario(?, ?, ?, ?, ?)}");
        cs.setInt(1, rolID);
        cs.setString(2, usuario);
        cs.setString(3, nombre);
        cs.setString(4, apellido);
        cs.setString(5, clave);
        cs.execute();
    }

    public void modificarUsuario(int usuarioID, int rolID, String usuario, String nombre, String apellido, String clave) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL MUsuario(?, ?, ?, ?, ?, ?)}");
        cs.setInt(1, usuarioID);
        cs.setInt(2, rolID);
        cs.setString(3, usuario);
        cs.setString(4, nombre);
        cs.setString(5, apellido);
        cs.setString(6, clave);
        cs.execute();
    }

    // TODO: obtenerClaveUsuario expone la contrasena en texto plano — ver issue #15
    // Se elimina cuando usuarios se migre a archivos (el hashing se implementa ahi)
    public String obtenerClaveUsuario(String usuario) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarUsuario(?)}");
        cs.setString(1, usuario);
        ResultSet rs = cs.executeQuery();
        if (rs.next()) return rs.getString("Clave");
        return "";
    }

    public void bajaUsuario(String nombreUsuario) throws SQLException {
        int usuarioID = obtenerUsuarioID(nombreUsuario);
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL BUsuario(?)}");
        cs.setInt(1, usuarioID);
        cs.execute();
    }

    public ResultSet listarUsuarios() throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ListarUsuarios()}");
        return cs.executeQuery();
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