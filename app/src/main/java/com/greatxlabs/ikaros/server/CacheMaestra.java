package com.greatxlabs.ikaros.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Mantiene en memoria los mapeos de IDs de tablas maestras (Roles, Estados).
 * Se carga una sola vez al iniciar el servidor para optimizar el rendimiento.
 */
public class CacheMaestra {

    private static final Map<String, Integer> roles = new HashMap<>();
    private static final Map<String, Integer> estadosMision = new HashMap<>();
    private static final Map<String, Integer> estadosTripulante = new HashMap<>();
    private static final Map<String, Integer> estadosEvento = new HashMap<>();
    private static final Map<String, Integer> aptitudes = new HashMap<>();

    private final AccesoDatos accesoDatos;

    public CacheMaestra(AccesoDatos accesoDatos) {
        this.accesoDatos = accesoDatos;
    }

    /**
     * Carga todos los datos maestros desde la base de datos.
     */
    public void cargarTodo() {
        System.out.println("Cargando tablas maestras en caché...");
        try {
            cargarMapa(roles, "Roles");
            cargarMapa(estadosMision, "EstadosMisiones");
            cargarMapa(estadosTripulante, "EstadosTripulantes");
            cargarMapa(estadosEvento, "EstadosEventos");
            cargarMapa(aptitudes, "Aptitudes");
            System.out.println("Carga de caché finalizada con éxito.");
        } catch (SQLException e) {
            System.err.println("Error cargando caché inicial: " + e.getMessage());
        }
    }

    private void cargarMapa(Map<String, Integer> mapa, String tabla) throws SQLException {
        try (ResultSet rs = accesoDatos.obtenerTablaMaestra(tabla)) {
            while (rs.next()) {
                mapa.put(rs.getString(2).toUpperCase(), rs.getInt(1));
            }
        }
    }

    public static Integer getRolID(String nombre) {
        return roles.get(nombre.toUpperCase());
    }

    public static Integer getEstadoMisionID(String nombre) {
        return estadosMision.get(nombre.toUpperCase());
    }

    public static Integer getEstadoTripulanteID(String nombre) {
        return estadosTripulante.get(nombre.toUpperCase());
    }

    public static Integer getEstadoEventoID(String nombre) {
        return estadosEvento.get(nombre.toUpperCase());
    }

    public static Integer getAptitudID(String nombre) {
        return aptitudes.get(nombre.toUpperCase());
    }
}
