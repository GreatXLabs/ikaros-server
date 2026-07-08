package com.greatxlabs.ikaros.server;

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
		cargarRoles();
		cargarEstadosMision();
		cargarEstadosTripulante();
		cargarEstadosEvento();
		cargarAptitudes();
		System.out.println("Carga de cache finalizada con exito.");
	}

	private void cargarRoles() {
		roles.putAll(accesoDatos.obtenerRolesComoMapa());
	}

	private void cargarAptitudes() {
		aptitudes.putAll(accesoDatos.obtenerAptitudesComoMapa());
	}

	private void cargarEstadosMision() {
		estadosMision.putAll(accesoDatos.obtenerEstadosMisionComoMapa());
	}

	private void cargarEstadosTripulante() {
		estadosTripulante.putAll(accesoDatos.obtenerEstadosTripulanteComoMapa());
	}

	private void cargarEstadosEvento() {
		estadosEvento.putAll(accesoDatos.obtenerEstadosEventoComoMapa());
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
