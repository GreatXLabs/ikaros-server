package com.greatxlabs.ikaros.server;

import java.sql.*;

public class Protocolo {

	private final GestorSesiones gestorSesiones;
	private final AccesoDatos accesoDatos;

	public Protocolo(GestorSesiones gestorSesiones, AccesoDatos accesoDatos) {
		this.gestorSesiones = gestorSesiones;
		this.accesoDatos = accesoDatos;
	}

	public String procesar(String solicitud) {
		if (solicitud == null || solicitud.isEmpty()) {
			return "ERROR|E99|Error interno del servidor";
		}

		String[] partes = solicitud.split("\\|", -1);
		String operacion = partes[0].toUpperCase();

		if (operacion.equals("LOGIN")) {
			return manejarLogin(partes);
		}

		if (partes.length < 2) {
			return "ERROR|E00|Sesión inválida o vencida";
		}

		String token = partes[1];

		// REGISTRAR_LOG no requiere sesion valida (el gateway maneja la autenticacion)
		if (operacion.equals("REGISTRAR_LOG")) {
			try {
				if (partes.length < 6) return "ERROR|E99|Parametros insuficientes";
				int usuarioID = Integer.parseInt(partes[2]);
				int accionID = Integer.parseInt(partes[3]);
				int tipoEntidadID = Integer.parseInt(partes[4]);
				int entidadID = Integer.parseInt(partes[5]);
				accesoDatos.registrarLog(usuarioID, accionID, tipoEntidadID, entidadID);
				return "OK|Log registrado";
			} catch (Exception e) {
				return "ERROR|E99|Error interno del servidor: " + e.getMessage();
			}
		}

		if (!gestorSesiones.esSesionValida(token)) {
			return "ERROR|E00|Sesión inválida o vencida";
		}

		if (!gestorSesiones.tienePermiso(token, operacion)) {
			return "ERROR|E01|Permiso insuficiente para esta operación";
		}

		try {
			switch (operacion) {
			// --- ROLES ---
			case "CONSULTAR_ROLES":
				return formatearLista(accesoDatos.consultarRoles(), 2);

			// --- APTITUDES ---
			case "CONSULTAR_APTITUDES":
				return formatearLista(accesoDatos.consultarAptitudes(), 2);

			// --- REGISTROS (LOGS) ---
			case "REGISTRAR_USUARIO": {
				if (partes.length < 7) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.registrarUsuario(
					CacheMaestra.getRolID(partes[6]),
					partes[2], partes[4], partes[5], partes[3]
				);
				return "OK|Usuario registrado";
			}

			// MODIFICAR_USUARIO|token|usuario|clave|nombre|apellido|rol
			case "MODIFICAR_USUARIO": {
				if (partes.length < 7) return "ERROR|E99|Parámetros insuficientes";
				int usuarioID = accesoDatos.obtenerUsuarioID(partes[2]);
				String clave = partes[3].isEmpty() ? accesoDatos.obtenerClaveUsuario(usuarioID) : partes[3];
				accesoDatos.modificarUsuario(
					usuarioID, CacheMaestra.getRolID(partes[6]),
					partes[2], partes[4], partes[5], clave
				);
				return "OK|Usuario modificado";
			}

			case "BAJA_USUARIO":
				if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.bajaUsuario(partes[2]);
				return "OK|Usuario dado de baja";

			case "LISTAR_USUARIOS":
				return formatearLista(accesoDatos.listarUsuarios(), 7);

			// --- MISIONES (COORDINADOR) ---
			case "REGISTRAR_MISION":
				if (partes.length < 7) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.registrarMision(
					CacheMaestra.getEstadoMisionID("PLANIFICADA"),
					partes[3], partes[4],
					parseTimestamp(partes[5]),
					parseTimestamp(partes[6])
				);
				return "OK|Misión registrada";

			case "MODIFICAR_MISION":
				if (partes.length < 7) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.modificarMision(
					Integer.parseInt(partes[2]),
					CacheMaestra.getEstadoMisionID(partes[3]),
					partes[4], partes[5],
					parseTimestamp(partes[6]),
					parseTimestamp(partes[7])
				);
				return "OK|Misión modificada";

			case "ACTUALIZAR_ESTADO_MISION":
				if (partes.length < 4) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.actualizarEstadoMision(Integer.parseInt(partes[2]), CacheMaestra.getEstadoMisionID(partes[3]));
				return "OK|Estado actualizado";

			case "LISTAR_MISIONES_ACTIVAS":
				return formatearLista(accesoDatos.listarMisionesActivas(), 3);

			case "CONSULTAR_MISION":
				return formatearDetalle(accesoDatos.consultarMision(Integer.parseInt(partes[2])), 8);

			// --- TRIPULANTES (ASIGNADOR) ---
			case "REGISTRAR_TRIPULANTE":
				if (partes.length < 9) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.registrarTripulante(
					CacheMaestra.getEstadoTripulanteID("ACTIVO"),
					obtenerSexoID(partes[2]),
					Integer.parseInt(partes[4]),
					Integer.parseInt(partes[5]),
					partes[6], partes[7],
					partes[8],
					Date.valueOf(partes[3])
				);
				return "OK|Tripulante registrado";

			case "MODIFICAR_TRIPULANTE":
				if (partes.length < 11) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.modificarTripulante(
					Integer.parseInt(partes[2]),
					CacheMaestra.getEstadoTripulanteID(partes[3]),
					obtenerSexoID(partes[4]),
					Integer.parseInt(partes[6]),
					Integer.parseInt(partes[7]),
					partes[8], partes[9],
					partes[10],
					Date.valueOf(partes[5])
				);
				return "OK|Tripulante modificado";

			case "BAJA_TRIPULANTE":
				if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.bajaTripulante(Integer.parseInt(partes[2]));
				return "OK|Tripulante dado de baja";

			case "ASIGNAR_TRIPULANTE":
				if (partes.length < 4) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.asignarTripulante(Integer.parseInt(partes[2]), Integer.parseInt(partes[3]), new Timestamp(System.currentTimeMillis()));
				return "OK|Tripulante asignado a misión";

			case "LISTAR_TRIPULANTES":
				return formatearLista(accesoDatos.listarTripulantes(), 8);

			case "CONSULTAR_TRIPULANTE":
				if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
				return formatearDetalle(accesoDatos.consultarTripulante(Integer.parseInt(partes[2])), 9);

			case "LISTAR_MISIONES_TRIPULANTE":
				if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
				return formatearLista(accesoDatos.listarMisionesTripulante(Integer.parseInt(partes[2])), 4);

			// --- EVENTOS (REGISTRADOR) ---
			case "REGISTRAR_EVENTO":
				if (partes.length < 5) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.registrarEvento(Integer.parseInt(partes[2]), partes[3], partes[4], new Timestamp(System.currentTimeMillis()));
				return "OK|Evento registrado";

			case "BAJA_EVENTO":
				if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
				accesoDatos.bajaEvento(Integer.parseInt(partes[2]));
				return "OK|Evento dado de baja";

			case "CONSULTAR_EVENTOS":
				if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
				return formatearLista(accesoDatos.consultarEventos(Integer.parseInt(partes[2])), 5);

			// --- LOGS (JEFE) ---
			case "VER_LOGS":
				return formatearLista(accesoDatos.verLogs(), 7);

			default:
				return "ERROR|E01|Permiso insuficiente para esta operación";
			}
		} catch (SQLException e) {
			return manejarErrorSQL(e);
		} catch (Exception e) {
			return "ERROR|E99|Error interno del servidor: " + e.getMessage();
		}
	}

	private int obtenerSexoID(String sexo) {
		if (sexo != null && sexo.toUpperCase().startsWith("F")) return 2;
		return 1;
	}

	private String manejarLogin(String[] partes) {
		if (partes.length < 3) return "ERROR|E02|Usuario o clave incorrectos";
		String res = gestorSesiones.iniciarSesion(partes[1], partes[2]);
		return (res != null) ? "OK|" + res : "ERROR|E02|Usuario o clave incorrectos";
	}

	private String manejarErrorSQL(SQLException e) {
		int code = e.getErrorCode();
		if (code == 1062) return "ERROR|E05|El ID ya existe en el sistema";
		if (code == 1452) return "ERROR|E07|El recurso solicitado no existe";

		System.err.println("SQL Error: " + e.getMessage());
		return "ERROR|E99|Error interno del servidor";
	}

	private String formatearLista(ResultSet rs, int columnas) throws SQLException {
		StringBuilder sb = new StringBuilder("OK|");
		boolean primero = true;
		while (rs.next()) {
			if (!primero) sb.append(";");
			for (int i = 1; i <= columnas; i++) {
				sb.append(rs.getString(i) != null ? rs.getString(i) : "");
				if (i < columnas) sb.append(":");
			}
			primero = false;
		}
		return sb.toString();
	}

	private String formatearDetalle(ResultSet rs, int columnas) throws SQLException {
		if (rs.next()) {
			StringBuilder sb = new StringBuilder("OK");
			for (int i = 1; i <= columnas; i++) {
				sb.append("|").append(rs.getString(i) == null ? "" : rs.getString(i));
			}
			return sb.toString();
		}
		return "ERROR|E07|El recurso solicitado no existe";
	}

	private Timestamp parseTimestamp(String value) {
		String datetime = value.replace("T", " ");
		if (!datetime.contains(":")) datetime += " 00:00:00";
		else if (datetime.split(":").length == 2) datetime += ":00";
		return Timestamp.valueOf(datetime);
	}
}
