package com.greatxlabs.ikaros.server;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Configuracion del servidor.
 *
 * Lee variables desde el archivo .env en el directorio de trabajo.
 * Si el archivo no existe (ej: en Docker/Dokploy), usa las variables
 * de entorno del sistema, que tienen prioridad en todos los casos.
 *
 * Variables soportadas:
 *   SERVER_PORT   puerto TCP del servidor (default: 9000)
 *   DB_URL        URL completa JDBC, ej: jdbc:mariadb://db:3306/ikaros
 *   DB_HOST       host de la base de datos (default: localhost)
 *   DB_PORT       puerto de la base de datos (default: 3306)
 *   DB_NAME       nombre de la base de datos (default: ikaros)
 *   DB_USER       usuario de la base de datos (default: root)
 *   DB_PASSWORD   contrasena de la base de datos (default: vacio)
 */
public class Configuracion {

	private static final Dotenv dotenv = Dotenv.configure()
		.ignoreIfMissing()
		.load();

	private static String get(String key, String defaultValue) {
		String value = dotenv.get(key);
		return (value != null && !value.isEmpty()) ? value : defaultValue;
	}

	public static int getPuerto() {
		return Integer.parseInt(get("SERVER_PORT", "9000"));
	}

	public static int getMaxClientes() {
		return Integer.parseInt(get("MAX_CLIENTS", "10"));
	}

	public static String getDbUrl() {
		String url = dotenv.get("DB_URL");
		if (url != null && !url.isEmpty()) return url;
		return "jdbc:mariadb://" + get("DB_HOST", "localhost") + ":" + get("DB_PORT", "3306") + "/" + get("DB_NAME", "ikaros");
	}

	public static String getDbUser() {
		return get("DB_USER", "root");
	}

	public static String getDbPassword() {
		return get("DB_PASSWORD", "");
	}
}
