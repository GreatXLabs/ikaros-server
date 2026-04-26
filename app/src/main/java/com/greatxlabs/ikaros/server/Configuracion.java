package com.greatxlabs.ikaros.server;

import java.util.Properties;
import java.io.*;

public class Configuracion {
    private static final String NOMBRE_ARCHIVO = "config.properties";
    private static final Properties props = new Properties();

    static {
        File archivo = new File(NOMBRE_ARCHIVO);

        if (!archivo.exists()) {
            System.out.println("No se encontró config.properties, generando con valores por defecto.");
            generarArchivoDefault(archivo);
        }

        try (InputStream input = new FileInputStream(archivo)) {
            props.load(input);
            System.out.println("Configuración cargada desde: " + archivo.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error cargando configuración: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void generarArchivoDefault(File archivo) {
        Properties defaults = new Properties();
        defaults.setProperty("server.port", "9000");
        defaults.setProperty("server.max_clients", "10");
        defaults.setProperty("db.host", "localhost");
        defaults.setProperty("db.port", "3306");
        defaults.setProperty("db.name", "ikaros");
        defaults.setProperty("db.user", "root");
        defaults.setProperty("db.password", "admin123");

        try (OutputStream output = new FileOutputStream(archivo)) {
            defaults.store(output, "Configuracion por defecto - generada automaticamente");
            System.out.println("config.properties creado correctamente.");
        } catch (IOException e) {
            System.err.println("No se pudo crear config.properties: " + e.getMessage());
            System.exit(1);
        }
    }

    public static int getPuerto() {
        return Integer.parseInt(props.getProperty("server.port", "9000"));
    }

    public static int getMaxClientes() {
        return Integer.parseInt(props.getProperty("server.max_clients", "10"));
    }

    public static String getDbHost() {
        return props.getProperty("db.host", "localhost");
    }

    public static int getDbPort() {
        return Integer.parseInt(props.getProperty("db.port", "3306"));
    }

    public static String getDbName() {
        return props.getProperty("db.name", "ikaros");
    }

    public static String getDbUrl() {
        String host = getDbHost();
        int port = getDbPort();
        String name = getDbName();
        return "jdbc:mariadb://" + host + ":" + port + "/" + name;
    }

    public static String getDbUser() {
        return props.getProperty("db.user", "root");
    }

    public static String getDbPassword() {
        return props.getProperty("db.password", "admin123");
    }
}
