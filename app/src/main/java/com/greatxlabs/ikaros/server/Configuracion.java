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

	public static String getDataDir() {
		return get("DATA_DIR", "data");
	}
}
