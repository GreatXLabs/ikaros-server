package com.greatxlabs.ikaros.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gestiona sesiones de usuario y permisos por rol.
 *
 * Sesiones: token UUID de 8 caracteres, expiran a los 30 min de inactividad.
 * Almacenadas en HashMap estatico — NO es thread-safe.
 *
 * Roles: JEFE (acceso total), RRHH, COORDINADOR, ASIGNADOR, REGISTRADOR.
 *
 * Recurso compartido: sesionesActivas debe sincronizarse para concurrencia.
 */
public class GestorSesiones {

	private static class Sesion {
		int usuarioID;
		String rol;
		long ultimaActividad;

		Sesion(int usuarioID, String rol) {
			this.usuarioID = usuarioID;
			this.rol = rol;
			this.ultimaActividad = System.currentTimeMillis();
		}

		void renovar() {
			this.ultimaActividad = System.currentTimeMillis();
		}

		boolean haExpirado() {
			long treintaMinutosEnMillis = 30 * 60 * 1000;
			return (System.currentTimeMillis() - ultimaActividad) > treintaMinutosEnMillis;
		}
	}

	private static final Map<String, Set<String>> PERMISOS_POR_ROL = new HashMap<>();
	static {
		PERMISOS_POR_ROL.put("RRHH", Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			"REGISTRAR_USUARIO", "MODIFICAR_USUARIO", "BAJA_USUARIO", "LISTAR_USUARIOS",
			"CONSULTAR_ROLES",
			"LISTAR_ESTADOS_MISIONES", "LISTAR_ESTADOS_TRIPULANTES", "LISTAR_ESTADOS_EVENTOS"
		))));
		PERMISOS_POR_ROL.put("COORDINADOR", Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			"REGISTRAR_MISION", "MODIFICAR_MISION", "ACTUALIZAR_ESTADO_MISION",
			"LISTAR_MISIONES", "CONSULTAR_MISION", "LISTAR_TRIPULANTES_MISION",
			"CONSULTAR_CAPACIDADES",
			"LISTAR_ESTADOS_MISIONES", "LISTAR_ESTADOS_TRIPULANTES", "LISTAR_ESTADOS_EVENTOS"
		))));
		PERMISOS_POR_ROL.put("ASIGNADOR", Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			"REGISTRAR_TRIPULANTE", "MODIFICAR_TRIPULANTE", "BAJA_TRIPULANTE",
			"ASIGNAR_TRIPULANTE", "REGISTRAR_CAPACIDAD", "ELIMINAR_CAPACIDADES",
			"LISTAR_TRIPULANTES", "CONSULTAR_TRIPULANTE", "CONSULTAR_CAPACIDADES", "CONSULTAR_APTITUDES",
			"LISTAR_EVENTOS", "CONSULTAR_EVENTOS", "LISTAR_MISIONES_TRIPULANTE",
			"LISTAR_TRIPULANTES_MISION", "LISTAR_MISIONES", "CONSULTAR_MISION",
			"LISTAR_ESTADOS_MISIONES", "LISTAR_ESTADOS_TRIPULANTES", "LISTAR_ESTADOS_EVENTOS"
		))));
		PERMISOS_POR_ROL.put("REGISTRADOR", Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			"LISTAR_EVENTOS", "REGISTRAR_EVENTO", "BAJA_EVENTO", "CONSULTAR_EVENTOS",
			"LISTAR_MISIONES", "CONSULTAR_MISION", "LISTAR_TRIPULANTES_MISION",
			"CONSULTAR_CAPACIDADES",
			"LISTAR_ESTADOS_MISIONES", "LISTAR_ESTADOS_TRIPULANTES", "LISTAR_ESTADOS_EVENTOS"
		))));
	}

	private static final Map<String, Sesion> sesionesActivas = new HashMap<>();

	private final AccesoDatos accesoDatos;

	public GestorSesiones(AccesoDatos accesoDatos) {
		this.accesoDatos = accesoDatos;
	}

	public String iniciarSesion(String usuario, String clave) {
		if (accesoDatos == null) return null;
		try {
			boolean credencialesValidas = accesoDatos.validarLogin(usuario, clave);
			if (!credencialesValidas) return null;

			try (java.sql.ResultSet rs = accesoDatos.obtenerDatosUsuario(usuario)) {
				if (rs.next()) {
					String token = UUID.randomUUID().toString().substring(0, 8);
					int usuarioID = rs.getInt("UsuarioID");
					String rol = rs.getString("NombreRol");

					sesionesActivas.put(token, new Sesion(usuarioID, rol.toUpperCase()));
					return token + "|" + rol.toUpperCase() + "|" + usuarioID;
				}
			}
		} catch (java.sql.SQLException e) {
			System.err.println("Error en validación de sesión: " + e.getMessage());
		}
		return null;
	}

	public boolean tienePermiso(String token, String operacion) {
		Sesion sesion = sesionesActivas.get(token);
		if (sesion == null) return false;

		if (sesion.haExpirado()) {
			sesionesActivas.remove(token);
			return false;
		}

		// Cualquier sesión válida puede registrar logs y consultar estados de referencia
		if (operacion.equals("REGISTRAR_LOG")) return true;

		String rol = sesion.rol;
		if (rol.equals("JEFE")) return true;

		Set<String> permitidas = PERMISOS_POR_ROL.get(rol);
		return permitidas != null && permitidas.contains(operacion);
	}

	public boolean esSesionValida(String token) {
		Sesion sesion = sesionesActivas.get(token);
		if (sesion == null) return false;

		if (sesion.haExpirado()) {
			sesionesActivas.remove(token);
			return false;
		}

		sesion.renovar();
		return true;
	}

	public String obtenerRol(String token) {
		Sesion sesion = sesionesActivas.get(token);
		if (sesion != null && !sesion.haExpirado()) {
			return sesion.rol;
		}
		return null;
	}

	public Integer obtenerUsuarioID(String token) {
		Sesion sesion = sesionesActivas.get(token);
		if (sesion != null && !sesion.haExpirado()) {
			return sesion.usuarioID;
		}
		return null;
	}
}
