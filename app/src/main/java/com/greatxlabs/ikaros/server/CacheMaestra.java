package com.greatxlabs.ikaros.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache en memoria de tablas de referencia (roles, estados, aptitudes).
 *
 * Se carga una vez al iniciar el servidor y nunca se invalidate.
 * Los lookups son por nombre (case-insensitive) y devuelven el ID numerico.
 *
 * Los metodos de lookup son estaticos, lo que dificulta testing.
 * Para concurrencia: los HashMaps estaticos son seguros si solo se leen
 * despues de la carga inicial (no se recargan en runtime).
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
			cargarDesdeResultSet(rs, roles);
		}
	}

	private void cargarAptitudes() throws SQLException {
		try (ResultSet rs = accesoDatos.consultarAptitudes()) {
			cargarDesdeResultSet(rs, aptitudes);
		}
	}

	private void cargarEstadosMision() throws SQLException {
		try (ResultSet rs = accesoDatos.listarEstadosMision()) {
			cargarDesdeResultSet(rs, estadosMision);
		}
	}

	private void cargarEstadosTripulante() throws SQLException {
		try (ResultSet rs = accesoDatos.listarEstadosTripulante()) {
			cargarDesdeResultSet(rs, estadosTripulante);
		}
	}

	private void cargarEstadosEvento() throws SQLException {
		try (ResultSet rs = accesoDatos.listarEstadosEvento()) {
			cargarDesdeResultSet(rs, estadosEvento);
		}
	}

	private static void cargarDesdeResultSet(ResultSet rs, Map<String, Integer> mapa) throws SQLException {
		while (rs.next()) {
			String col2 = rs.getString(2);
			if (col2 == null) continue;
			int id;
			try {
				id = rs.getInt(1);
			} catch (Exception e) {
				continue;
			}
			mapa.put(col2.toUpperCase(), id);
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
