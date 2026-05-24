package com.greatxlabs.ikaros.server;

import java.util.Properties;
import java.io.*;

/**
 * Configuracion del servidor.
 *
 * Prioridad de lectura: variable de entorno > config.properties > valor por defecto.
 * Las variables de entorno permiten configurar el servidor en Dokploy/Docker
 * sin necesidad de archivos de configuracion.
 *
 * Variables de entorno soportadas:
 *   SERVER_PORT       — puerto TCP del servidor (default: 9000)
 *   DB_URL            — URL completa de conexion JDBC (ej: jdbc:mariadb://db:3306/ikaros)
 *   DB_HOST           — host de la base de datos (default: localhost)
 *   DB_PORT           — puerto de la base de datos (default: 3306)
 *   DB_NAME           — nombre de la base de datos (default: ikaros)
 *   DB_USER           — usuario de la base de datos (default: root)
 *   DB_PASSWORD       — contraseña de la base de datos (default: admin123)
 */
public class Configuracion {
	private static final String NOMBRE_ARCHIVO = "config.properties";
	private static final Properties props = new Properties();

	static {
		File archivo = new File(NOMBRE_ARCHIVO);

		if (!archivo.exists()) {
			System.out.println("No se encontro config.properties, usando variables de entorno o valores por defecto.");
		} else {
			try (InputStream input = new FileInputStream(archivo)) {
				props.load(input);
				System.out.println("Configuracion cargada desde: " + archivo.getAbsolutePath());
			} catch (IOException e) {
				System.err.println("Error cargando configuracion: " + e.getMessage());
			}
		}
	}

	private static String getEnvOrProp(String envVar, String propKey, String defaultValue) {
		String env = System.getenv(envVar);
		if (env != null && !env.isEmpty()) return env;
		return props.getProperty(propKey, defaultValue);
	}

	public static int getPuerto() {
		return Integer.parseInt(getEnvOrProp("SERVER_PORT", "server.port", "9000"));
	}

	public static int getMaxClientes() {
		return Integer.parseInt(getEnvOrProp("MAX_CLIENTS", "server.max_clients", "10"));
	}

	public static String getDbHost() {
		return getEnvOrProp("DB_HOST", "db.host", "localhost");
	}

	public static int getDbPort() {
		return Integer.parseInt(getEnvOrProp("DB_PORT", "db.port", "3306"));
	}

	public static String getDbName() {
		return getEnvOrProp("DB_NAME", "db.name", "ikaros");
	}

	public static String getDbUrl() {
		String envUrl = System.getenv("DB_URL");
		if (envUrl != null && !envUrl.isEmpty()) return envUrl;
		return "jdbc:mariadb://" + getDbHost() + ":" + getDbPort() + "/" + getDbName();
	}

	public static String getDbUser() {
		return getEnvOrProp("DB_USER", "db.user", "root");
	}

	public static String getDbPassword() {
		return getEnvOrProp("DB_PASSWORD", "db.password", "admin123");
	}
}
