package com.greatxlabs.ikaros.server;

import java.sql.*;

/**
 * Encapsula todas las operaciones de acceso a datos mediante Stored Procedures.
 * No se ejecutan sentencias SQL directas (SELECT/INSERT/UPDATE) aquí.
 */
public class AccesoDatos {

    // --- AUTENTICACIÓN Y SESIÓN ---
    public ResultSet validarLogin(String usuario, String clave) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ValidarLogin(?, ?)}");
        cs.setString(1, usuario);
        cs.setString(2, clave);
        return cs.executeQuery();
    }

    public ResultSet obtenerTablaMaestra(String tipoTabla) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ObtenerTablaMaestra(?)}");
        cs.setString(1, tipoTabla);
        return cs.executeQuery();
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

    public void bajaUsuario(int usuarioID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL AEUsuario(?, 0)}"); // 0 = Estado Inactivo/Baja
        cs.setInt(1, usuarioID);
        cs.execute();
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

    public void actualizarEstadoMision(int id, int estadoID) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL AEMision(?, ?)}");
        cs.setInt(1, id);
        cs.setInt(2, estadoID);
        cs.execute();
    }

    public ResultSet listarMisionesActivas() throws SQLException {
        return ConexionBD.getConexion().prepareCall("{CALL ListarMisionesActivas()}").executeQuery();
    }

    public ResultSet consultarMision(int id) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ConsultarMision(?)}");
        cs.setInt(1, id);
        return cs.executeQuery();
    }

    // --- TRIPULANTES ---
    public void registrarTripulante(int estadoID, int peso, int altura, String nom, String ape, Date nac) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ATripulante(?, ?, ?, ?, ?, ?)}");
        cs.setInt(1, estadoID);
        cs.setInt(2, peso);
        cs.setInt(3, altura);
        cs.setString(4, nom);
        cs.setString(5, ape);
        cs.setDate(6, nac);
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
        return ConexionBD.getConexion().prepareCall("{CALL ListarTripulantes()}").executeQuery();
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

    public ResultSet verLogs() throws SQLException {
        return ConexionBD.getConexion().prepareCall("{CALL VerLogs()}").executeQuery();
    }
}
