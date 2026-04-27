package com.greatxlabs.ikaros.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

	public void cargarTodo() {
		System.out.println("Cargando tablas maestras en cache...");
		try {
			cargarRoles();
			cargarEstadosMision();
			cargarEstadosTripulante();
			cargarEstadosEvento();
			cargarAptitudes();
			System.out.println("Carga de cache finalizada con exito.");
		} catch (SQLException e) {
			System.err.println("Error cargando cache inicial: " + e.getMessage());
		}
	}

	private void cargarRoles() throws SQLException {
		try (ResultSet rs = accesoDatos.consultarRoles()) {
			while (rs.next()) {
				roles.put(rs.getString(2).toUpperCase(), rs.getInt(1));
			}
		}
	}

	private void cargarAptitudes() throws SQLException {
		try (ResultSet rs = accesoDatos.consultarAptitudes()) {
			while (rs.next()) {
				aptitudes.put(rs.getString(2).toUpperCase(), rs.getInt(1));
			}
		}
	}

	private void cargarEstadosMision() throws SQLException {
		try (ResultSet rs = accesoDatos.listarEstadosMision()) {
			while (rs.next()) {
				estadosMision.put(rs.getString(2).toUpperCase(), rs.getInt(1));
			}
		}
	}

	private void cargarEstadosTripulante() throws SQLException {
		try (ResultSet rs = accesoDatos.listarEstadosTripulante()) {
			while (rs.next()) {
				estadosTripulante.put(rs.getString(2).toUpperCase(), rs.getInt(1));
			}
		}
	}

	private void cargarEstadosEvento() throws SQLException {
		try (ResultSet rs = accesoDatos.listarEstadosEvento()) {
			while (rs.next()) {
				estadosEvento.put(rs.getString(2).toUpperCase(), rs.getInt(1));
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
