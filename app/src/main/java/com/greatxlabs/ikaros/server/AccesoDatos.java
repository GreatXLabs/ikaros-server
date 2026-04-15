package com.greatxlabs.ikaros.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsula todas las operaciones de acceso a datos mediante Stored Procedures.
 * Sigue la regla de no ejecutar sentencias SQL directas en el código.
 */
public class AccesoDatos {

    /**
     * Valida las credenciales de un usuario.
     * 
     * @param usuario Nombre de usuario.
     * @param clave Clave de acceso.
     * @return ResultSet con los datos del usuario si es válido (UsuarioID, RolID, Nombre).
     * @throws SQLException Si ocurre un error en la base de datos.
     */
    public ResultSet validarLogin(String usuario, String clave) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ValidarLogin(?, ?)}");
        cs.setString(1, usuario);
        cs.setString(2, clave);
        return cs.executeQuery();
    }

    /**
     * Registra una nueva misión en el sistema.
     */
    public void registrarMision(int estadoMID, String nombre, String descripcion, 
                                 Timestamp fechaIniEst, Timestamp fechaFinEst) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL AMision(?, ?, ?, ?, ?)}");
        cs.setInt(1, estadoMID);
        cs.setString(2, nombre);
        cs.setString(3, descripcion);
        cs.setTimestamp(4, fechaIniEst);
        cs.setTimestamp(5, fechaFinEst);
        cs.execute();
    }

    /**
     * Registra un nuevo tripulante.
     */
    public void registrarTripulante(int estadoTID, int peso, int altura, 
                                     String nombre, String apellido, Date fechaNac) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ATripulante(?, ?, ?, ?, ?, ?)}");
        cs.setInt(1, estadoTID);
        cs.setInt(2, peso);
        cs.setInt(3, altura);
        cs.setString(4, nombre);
        cs.setString(5, apellido);
        cs.setDate(6, fechaNac);
        cs.execute();
    }

    /**
     * Obtiene las tablas maestras de IDs (Roles, Estados, etc.) para la caché.
     * 
     * @param tipoTabla El nombre de la tabla de referencia a consultar.
     * @return ResultSet con ID y Nombre/Descripcion.
     */
    public ResultSet obtenerTablaMaestra(String tipoTabla) throws SQLException {
        Connection con = ConexionBD.getConexion();
        CallableStatement cs = con.prepareCall("{CALL ObtenerTablaMaestra(?)}");
        cs.setString(1, tipoTabla);
        return cs.executeQuery();
    }

    // Aquí se irán añadiendo el resto de métodos para cada SP (AMision, MMision, etc.)
}
