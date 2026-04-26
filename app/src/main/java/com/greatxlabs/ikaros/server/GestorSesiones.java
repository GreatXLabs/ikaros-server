package com.greatxlabs.ikaros.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

	private static final Map<String, Sesion> sesionesActivas = new HashMap<>();

	private final AccesoDatos accesoDatos;

	public GestorSesiones(AccesoDatos accesoDatos) {
		this.accesoDatos = accesoDatos;
	}

	public String iniciarSesion(String usuario, String clave) {
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

		// REGISTRAR_LOG lo puede hacer cualquier sesión válida
		if (operacion.equals("REGISTRAR_LOG")) return true;

		String rol = sesion.rol;
		if (rol.equals("JEFE")) return true;

		switch (rol) {
		case "RRHH":
			return operacion.equals("REGISTRAR_USUARIO") ||
				operacion.equals("MODIFICAR_USUARIO") ||
				operacion.equals("BAJA_USUARIO") ||
				operacion.equals("LISTAR_USUARIOS") ||
				operacion.equals("CONSULTAR_ROLES");

		case "COORDINADOR":
			return operacion.equals("REGISTRAR_MISION") ||
				operacion.equals("MODIFICAR_MISION") ||
				operacion.equals("ACTUALIZAR_ESTADO_MISION") ||
				operacion.equals("LISTAR_MISIONES_ACTIVAS") ||
				operacion.equals("CONSULTAR_MISION");

		case "ASIGNADOR":
			return operacion.equals("REGISTRAR_TRIPULANTE") ||
				operacion.equals("MODIFICAR_TRIPULANTE") ||
				operacion.equals("BAJA_TRIPULANTE") ||
				operacion.equals("ASIGNAR_TRIPULANTE") ||
				operacion.equals("LISTAR_TRIPULANTES") ||
				operacion.equals("CONSULTAR_TRIPULANTE") ||
					operacion.equals("CONSULTAR_APTITUDES");

		case "REGISTRADOR":
			return operacion.equals("REGISTRAR_EVENTO") ||
				operacion.equals("BAJA_EVENTO") ||
				operacion.equals("CONSULTAR_EVENTOS");

		default:
			return false;
		}
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
