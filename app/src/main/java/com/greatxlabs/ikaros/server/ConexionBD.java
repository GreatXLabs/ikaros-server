package com.greatxlabs.ikaros.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona la conexión con la base de datos MariaDB.
 * Utiliza los parámetros definidos en la configuración.
 */
public class ConexionBD {
    
    private static Connection conexion = null;

    /**
     * Obtiene una conexión activa a la base de datos.
     * Al ser un servidor secuencial, podemos mantener una única conexión persistente.
     * 
     * @return Connection objeto de conexión.
     * @throws SQLException Si ocurre un error al conectar.
     */
    public static Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            String url = Configuracion.getDbUrl();
            String user = Configuracion.getDbUser();
            String pass = Configuracion.getDbPassword();
            
            conexion = DriverManager.getConnection(url, user, pass);
            System.out.println("Conexión establecida con la base de datos MariaDB.");
        }
        return conexion;
    }

    /**
     * Cierra la conexión actual si está abierta.
     */
    public static void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                System.out.println("Conexión con la base de datos cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }
}
