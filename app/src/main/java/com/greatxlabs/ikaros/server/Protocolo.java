package com.greatxlabs.ikaros.server;

import java.sql.*;

public class Protocolo {

	private static class ErrorProtocolo extends RuntimeException {
		final String respuesta;
		ErrorProtocolo(String respuesta) { super(respuesta); this.respuesta = respuesta; }
	}

	private final GestorSesiones gestorSesiones;
	private final AccesoDatos accesoDatos;

	public Protocolo(GestorSesiones gestorSesiones, AccesoDatos accesoDatos) {
		this.gestorSesiones = gestorSesiones;
		this.accesoDatos = accesoDatos;
	}

	public String procesar(String solicitud) {
		String resultado = procesarInterno(solicitud);
		LogSistema.registrar("RESULTADO " + resultado);
		return resultado;
	}

	private String procesarInterno(String solicitud) {
		if (solicitud == null || solicitud.isEmpty()) {
			return "ERROR|E99|Error interno del servidor";
		}

		String[] partes = solicitud.split("\\|", -1);
		String operacion = partes[0].toUpperCase();
		String tokenLog = partes.length > 1 ? partes[1] : "-";
		LogSistema.registrar("OPERACION " + operacion + " token=" + tokenLog);

		if (operacion.equals("LOGIN")) {
			return manejarLogin(partes);
		}

		if (partes.length < 2) {
			return "ERROR|E00|Sesión inválida o vencida";
		}

		String token = partes[1];

		if (!gestorSesiones.esSesionValida(token)) {
			return "ERROR|E00|Sesión inválida o vencida";
		}

		if (!gestorSesiones.tienePermiso(token, operacion)) {
			return "ERROR|E01|Permiso insuficiente para esta operación";
		}

		try {
			switch (operacion) {
			case "CONSULTAR_ROLES":
				return formatearLista(accesoDatos.consultarRoles(), 2);
			case "CONSULTAR_APTITUDES":
				return formatearLista(accesoDatos.consultarAptitudes(), 2);
			case "LISTAR_ESTADOS_MISIONES":
				return formatearLista(accesoDatos.listarEstadosMision(), 2);
			case "LISTAR_ESTADOS_TRIPULANTES":
				return formatearLista(accesoDatos.listarEstadosTripulante(), 2);
			case "LISTAR_ESTADOS_EVENTOS":
				return formatearLista(accesoDatos.listarEstadosEvento(), 2);
			case "REGISTRAR_LOG":
				return manejarRegistroLog(token, partes);
			case "REGISTRAR_USUARIO":
			case "MODIFICAR_USUARIO":
			case "BAJA_USUARIO":
			case "LISTAR_USUARIOS":
				return manejarUsuarios(operacion, partes, token);
			case "REGISTRAR_MISION":
			case "MODIFICAR_MISION":
			case "ACTUALIZAR_ESTADO_MISION":
			case "LISTAR_MISIONES":
			case "CONSULTAR_MISION":
				return manejarMisiones(operacion, partes, token);
			case "REGISTRAR_TRIPULANTE":
			case "MODIFICAR_TRIPULANTE":
			case "BAJA_TRIPULANTE":
			case "ELIMINAR_CAPACIDADES":
			case "REGISTRAR_CAPACIDAD":
			case "ASIGNAR_TRIPULANTE":
			case "LISTAR_TRIPULANTES":
			case "LISTAR_TRIPULANTES_MISION":
			case "CONSULTAR_TRIPULANTE":
			case "CONSULTAR_CAPACIDADES":
			case "LISTAR_MISIONES_TRIPULANTE":
				return manejarTripulantes(operacion, partes, token);
			case "LISTAR_EVENTOS":
			case "REGISTRAR_EVENTO":
			case "BAJA_EVENTO":
			case "CONSULTAR_EVENTOS":
			case "VER_LOGS":
				return manejarEventos(operacion, partes, token);
			default:
				return "ERROR|E01|Permiso insuficiente para esta operación";
			}
		} catch (ErrorProtocolo e) {
			return e.respuesta;
		} catch (SQLException e) {
			return manejarErrorSQL(e);
		} catch (Exception e) {
			return "ERROR|E99|Error interno del servidor: " + e.getMessage();
		}
	}

	private String manejarRegistroLog(String token, String[] partes) {
		if (partes.length < 5) return "ERROR|E99|Parámetros insuficientes";
		int accionID = parseEntero(partes[2], "accionID");
		int tipoEntidadID = parseEntero(partes[3], "tipoEntidadID");
		int entidadID = parseEntero(partes[4], "entidadID");
		String descripcion = partes.length > 5 ? partes[5] : "";
		Integer usuarioID = gestorSesiones.obtenerUsuarioID(token);
		if (usuarioID == null) return "ERROR|E00|Sesión inválida o vencida";
		accesoDatos.registrarLog(usuarioID, accionID, tipoEntidadID, entidadID, descripcion);
		return "OK|Log registrado";
	}

	private String manejarUsuarios(String operacion, String[] partes, String token) throws SQLException {
		Integer loggedInUserID = gestorSesiones.obtenerUsuarioID(token);
		if (loggedInUserID == null) return "ERROR|E00|Sesión inválida o vencida";
		switch (operacion) {
		case "REGISTRAR_USUARIO": {
			if (partes.length < 7) return "ERROR|E99|Parámetros insuficientes";
			String usuario = partes[2];
			String clave = partes[3];
			String nombre = partes[4];
			String apellido = partes[5];
			String rol = partes[6];
			int nuevoId = accesoDatos.registrarUsuario(CacheMaestra.getRolID(rol), usuario, nombre, apellido, clave);
			String descLog = "Usuario=" + usuario + "|Nombre=" + nombre + "|Apellido=" + apellido + "|Rol=" + rol;
			accesoDatos.registrarLog(loggedInUserID, 13, 4, nuevoId, descLog);
			return "OK|Usuario registrado";
		}
		case "MODIFICAR_USUARIO": {
			if (partes.length < 8) return "ERROR|E99|Parámetros insuficientes";
			int usuarioID = parseEntero(partes[2], "usuarioID");
			String usuario = partes[3];
			String clave = partes[4].isEmpty() ? null : partes[4];
			String nombre = partes[5];
			String apellido = partes[6];
			String rol = partes[7];
			accesoDatos.modificarUsuario(loggedInUserID, usuarioID, CacheMaestra.getRolID(rol), usuario, nombre, apellido, clave);
			return "OK|Usuario modificado";
		}
		case "BAJA_USUARIO": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			accesoDatos.bajaUsuario(loggedInUserID, partes[2]);
			return "OK|Usuario dado de baja";
		}
		case "LISTAR_USUARIOS":
			return formatearLista(accesoDatos.listarUsuarios(), 8);
		default:
			return "ERROR|E01|Permiso insuficiente para esta operación";
		}
	}

	private String manejarMisiones(String operacion, String[] partes, String token) throws SQLException {
		Integer loggedInUserID = gestorSesiones.obtenerUsuarioID(token);
		if (loggedInUserID == null) return "ERROR|E00|Sesión inválida o vencida";
		switch (operacion) {
		case "REGISTRAR_MISION": {
			if (partes.length < 7) return "ERROR|E99|Parámetros insuficientes";
			String nombre = partes[3];
			String descripcion = partes[4];
			Timestamp fechaInicio = parseTimestamp(partes[5], "fechaInicio");
			Timestamp fechaFin = parseTimestamp(partes[6], "fechaFin");
			int nuevoId = accesoDatos.registrarMision(CacheMaestra.getEstadoMisionID("PLANIFICADA"), nombre, descripcion, fechaInicio, fechaFin);
			String descLog = "Nombre=" + nombre + "|Descripcion=" + descripcion + "|FechaInicio=" + fechaInicio + "|FechaFin=" + fechaFin;
			accesoDatos.registrarLog(loggedInUserID, 1, 1, nuevoId, descLog);
			return "OK|Misión registrada";
		}
		case "MODIFICAR_MISION": {
			if (partes.length < 7) return "ERROR|E99|Parámetros insuficientes";
			int misionID = parseEntero(partes[2], "misionID");
			String nombre = partes[3];
			String descripcion = partes[4];
			Timestamp fechaInicio = parseTimestamp(partes[5], "fechaInicio");
			Timestamp fechaFin = parseTimestamp(partes[6], "fechaFin");
			if (!accesoDatos.existeMision(misionID))
				return "ERROR|E07|La misión con ID " + misionID + " no existe";
			accesoDatos.modificarMision(loggedInUserID, misionID, nombre, descripcion, fechaInicio, fechaFin);
			return "OK|Misión modificada";
		}
		case "ACTUALIZAR_ESTADO_MISION": {
			if (partes.length < 4) return "ERROR|E99|Parámetros insuficientes";
			int misionID = parseEntero(partes[2], "misionID");
			String estado = partes[3];
			Integer retrasoInicio = partes.length > 4 && !partes[4].isEmpty() ? parseEntero(partes[4], "retrasoInicio") : null;
			Integer retrasoFin = partes.length > 5 && !partes[5].isEmpty() ? parseEntero(partes[5], "retrasoFin") : null;
			if (!accesoDatos.existeMision(misionID))
				return "ERROR|E07|La misión con ID " + misionID + " no existe";
			accesoDatos.actualizarEstadoMision(loggedInUserID, misionID, CacheMaestra.getEstadoMisionID(estado), retrasoInicio, retrasoFin);
			return "OK|Estado actualizado";
		}
		case "LISTAR_MISIONES":
			return formatearLista(accesoDatos.listarMisiones(), 7);
		case "CONSULTAR_MISION": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			int misionID = parseEntero(partes[2], "misionID");
			return formatearDetalle(accesoDatos.consultarMision(misionID), 8);
		}
		default:
			return "ERROR|E01|Permiso insuficiente para esta operación";
		}
	}

	private String manejarTripulantes(String operacion, String[] partes, String token) throws SQLException {
		Integer loggedInUserID = gestorSesiones.obtenerUsuarioID(token);
		if (loggedInUserID == null) return "ERROR|E00|Sesión inválida o vencida";
		switch (operacion) {
		case "REGISTRAR_TRIPULANTE": {
			if (partes.length < 9) return "ERROR|E99|Parámetros insuficientes";
			String sexo = partes[2];
			Date fechaNacimiento = parseFecha(partes[3], "fechaNacimiento");
			int peso = parseEntero(partes[4], "peso");
			int altura = parseEntero(partes[5], "altura");
			String nombre = partes[6];
			String apellido = partes[7];
			String imagen = partes[8];
			ResultSet rs = accesoDatos.registrarTripulante(
				CacheMaestra.getEstadoTripulanteID("INACTIVO"), obtenerSexoID(sexo),
				peso, altura, nombre, apellido, imagen, fechaNacimiento
			);
			if (rs.next()) {
				int nuevoId = rs.getInt(1);
				String descLog = "Nombre=" + nombre + "|Apellido=" + apellido + "|Sexo=" + sexo + "|Peso=" + peso + "|Altura=" + altura;
				accesoDatos.registrarLog(loggedInUserID, 8, 2, nuevoId, descLog);
			}
			return formatearDetalle(rs, 1);
		}
		case "MODIFICAR_TRIPULANTE": {
			if (partes.length < 11) return "ERROR|E99|Parámetros insuficientes";
			int tripulanteID = parseEntero(partes[2], "tripulanteID");
			String estado = partes[3];
			String sexo = partes[4];
			Date fechaNacimiento = parseFecha(partes[5], "fechaNacimiento");
			int peso = parseEntero(partes[6], "peso");
			int altura = parseEntero(partes[7], "altura");
			String nombre = partes[8];
			String apellido = partes[9];
			String imagen = partes[10];
			if (!accesoDatos.existeTripulante(tripulanteID))
				return "ERROR|E07|El tripulante con ID " + tripulanteID + " no existe";
			accesoDatos.modificarTripulante(loggedInUserID, tripulanteID, CacheMaestra.getEstadoTripulanteID(estado),
				obtenerSexoID(sexo), peso, altura, nombre, apellido, imagen, fechaNacimiento);
			return "OK|Tripulante modificado";
		}
		case "BAJA_TRIPULANTE": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			int tripulanteID = parseEntero(partes[2], "tripulanteID");
			if (!accesoDatos.existeTripulante(tripulanteID))
				return "ERROR|E07|El tripulante con ID " + tripulanteID + " no existe";
			accesoDatos.bajaTripulante(loggedInUserID, tripulanteID);
			return "OK|Tripulante dado de baja";
		}
		case "ELIMINAR_CAPACIDADES": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			int tripulanteID = parseEntero(partes[2], "tripulanteID");
			accesoDatos.eliminarCapacidades(tripulanteID);
			return "OK|Capacidades eliminadas";
		}
		case "REGISTRAR_CAPACIDAD": {
			if (partes.length < 6) return "ERROR|E99|Parámetros insuficientes";
			int tripulanteID = parseEntero(partes[2], "tripulanteID");
			int aptitudID = parseEntero(partes[3], "aptitudID");
			int calificacion = parseEntero(partes[4], "calificacion");
			String fecha = partes[5];
			accesoDatos.registrarCapacidad(loggedInUserID, tripulanteID, aptitudID, calificacion, fecha);
			return "OK|Capacidad registrada";
		}
		case "ASIGNAR_TRIPULANTE": {
			if (partes.length < 4) return "ERROR|E99|Parámetros insuficientes";
			int tripulanteID = parseEntero(partes[2], "tripulanteID");
			int misionID = parseEntero(partes[3], "misionID");
			if (!accesoDatos.existeTripulante(tripulanteID))
				return "ERROR|E07|El tripulante con ID " + tripulanteID + " no existe";
			if (!accesoDatos.existeMision(misionID))
				return "ERROR|E07|La misión con ID " + misionID + " no existe";
			accesoDatos.asignarTripulante(loggedInUserID, tripulanteID, misionID, new Timestamp(System.currentTimeMillis()));
			return "OK|Tripulante asignado a misión";
		}
		case "LISTAR_TRIPULANTES":
			return formatearLista(accesoDatos.listarTripulantes(), 8);
		case "LISTAR_TRIPULANTES_MISION": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			int misionID = parseEntero(partes[2], "misionID");
			return formatearLista(accesoDatos.listarTripulantesMision(misionID), 5);
		}
		case "CONSULTAR_TRIPULANTE": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			int tripulanteID = parseEntero(partes[2], "tripulanteID");
			return formatearDetalle(accesoDatos.consultarTripulante(tripulanteID), 9);
		}
		case "CONSULTAR_CAPACIDADES": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			int tripulanteID = parseEntero(partes[2], "tripulanteID");
			if (!accesoDatos.existeTripulante(tripulanteID))
				return "ERROR|E07|El tripulante con ID " + tripulanteID + " no existe";
			return formatearLista(accesoDatos.consultarCapacidades(tripulanteID), 4);
		}
		case "LISTAR_MISIONES_TRIPULANTE": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			int tripulanteID = parseEntero(partes[2], "tripulanteID");
			return formatearLista(accesoDatos.listarMisionesTripulante(tripulanteID), 5);
		}
		default:
			return "ERROR|E01|Permiso insuficiente para esta operación";
		}
	}

	private String manejarEventos(String operacion, String[] partes, String token) throws SQLException {
		Integer loggedInUserID = gestorSesiones.obtenerUsuarioID(token);
		if (loggedInUserID == null) return "ERROR|E00|Sesión inválida o vencida";
		switch (operacion) {
		case "LISTAR_EVENTOS":
			return formatearLista(accesoDatos.listarEventos(), 6);
		case "REGISTRAR_EVENTO": {
			if (partes.length < 5) return "ERROR|E99|Parámetros insuficientes";
			int misionID = parseEntero(partes[2], "misionID");
			String titulo = partes[3];
			String descripcion = partes[4];
			if (!accesoDatos.existeMision(misionID))
				return "ERROR|E07|La misión con ID " + misionID + " no existe";
			accesoDatos.registrarEvento(loggedInUserID, misionID, titulo, descripcion, new Timestamp(System.currentTimeMillis()));
			return "OK|Evento registrado";
		}
		case "BAJA_EVENTO": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			int eventoID = parseEntero(partes[2], "eventoID");
			accesoDatos.bajaEvento(loggedInUserID, eventoID);
			return "OK|Evento dado de baja";
		}
		case "CONSULTAR_EVENTOS": {
			if (partes.length < 3) return "ERROR|E99|Parámetros insuficientes";
			int misionID = parseEntero(partes[2], "misionID");
			return formatearLista(accesoDatos.consultarEventos(misionID), 5);
		}
		case "VER_LOGS":
			return formatearLista(accesoDatos.verLogs(), 8);
		default:
			return "ERROR|E01|Permiso insuficiente para esta operación";
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

	private int parseEntero(String valor, String nombre) {
		try {
			return Integer.parseInt(valor);
		} catch (NumberFormatException e) {
			throw new ErrorProtocolo("ERROR|E10|El parámetro '" + nombre + "' debe ser un número entero");
		}
	}

	private Date parseFecha(String valor, String nombre) {
		try {
			return Date.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new ErrorProtocolo("ERROR|E10|El parámetro '" + nombre + "' tiene formato de fecha inválido (esperado: YYYY-MM-DD)");
		}
	}

	private Timestamp parseTimestamp(String valor, String nombre) {
		try {
			String dt = valor.replace("T", " ");
			if (!dt.contains(":")) dt += " 00:00:00";
			else if (dt.split(":").length == 2) dt += ":00";
			return Timestamp.valueOf(dt);
		} catch (IllegalArgumentException e) {
			throw new ErrorProtocolo("ERROR|E10|El parámetro '" + nombre + "' tiene formato de fecha inválido (esperado: YYYY-MM-DD o YYYY-MM-DDTHH:MM:SS)");
		}
	}

	private String formatearLista(ResultSet rs, int columnas) throws SQLException {
		StringBuilder sb = new StringBuilder("OK|");
		boolean primero = true;
		while (rs.next()) {
			String firstCol = rs.getString(1);
			if (firstCol != null && (firstCol.startsWith("Exito:") || firstCol.startsWith("Error:"))) continue;
			if (!primero) sb.append(";");
			for (int i = 1; i <= columnas; i++) {
				String val = rs.getString(i);
				sb.append(val != null ? val : "");
				if (i < columnas) sb.append("~");
			}
			primero = false;
		}
		return sb.toString();
	}

	private String formatearDetalle(ResultSet rs, int columnas) throws SQLException {
		if (rs.next()) {
			StringBuilder sb = new StringBuilder("OK");
			for (int i = 1; i <= columnas; i++) {
				String val = rs.getString(i);
				sb.append("|").append(val == null ? "" : val);
			}
			return sb.toString();
		}
		return "ERROR|E07|El recurso solicitado no existe";
	}
}
