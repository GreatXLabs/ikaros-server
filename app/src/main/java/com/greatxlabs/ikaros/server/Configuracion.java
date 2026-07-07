package com.greatxlabs.ikaros.server;

import io.github.cdimascio.dotenv.Dotenv;

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


	public static String getDataDir() {
		return get("DATA_DIR", "data");
	}
}
