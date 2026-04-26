package com.greatxlabs.ikaros.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

	private static Connection conexion = null;

	public static Connection getConexion() throws SQLException {
		if (conexion == null || conexion.isClosed()) {
			String url = Configuracion.getDbUrl();
			String user = Configuracion.getDbUser();
			String pass = Configuracion.getDbPassword();


			try {
				conexion = DriverManager.getConnection(url, user, pass);
				System.out.println("Conexión establecida con la base de datos MariaDB.");
			} catch (SQLException e) {
				System.err.println("[ERROR] Conexión fallida: " + e.getMessage());
				System.err.println("[ERROR] SQLState: " + e.getSQLState());
				System.err.println("[ERROR] ErrorCode: " + e.getErrorCode());
				throw e;
			}
		}
		return conexion;
	}

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
