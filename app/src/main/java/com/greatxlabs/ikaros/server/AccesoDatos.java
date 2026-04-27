package com.greatxlabs.ikaros.server;

import java.sql.*;

public class AccesoDatos {

	// --- AUTENTICACIÓN Y SESIÓN ---
	public boolean validarLogin(String usuario, String clave) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{? = CALL ValidarLogin(?, ?)}");
		cs.registerOutParameter(1, java.sql.Types.BOOLEAN);
		cs.setString(2, usuario);
		cs.setString(3, clave);
		cs.execute();
		return cs.getBoolean(1);
	}

	public ResultSet obtenerDatosUsuario(String usuario) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ConsultarUsuario(?)}");
		cs.setString(1, usuario);
		return cs.executeQuery();
	}


	// --- ROLES ---
	public ResultSet consultarRoles() throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ConsultarRoles()}");
		return cs.executeQuery();
	}

	public ResultSet consultarAptitudes() throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ListarAptitudes()}");
		return cs.executeQuery();
	}

	public ResultSet listarEstadosMision() throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ListarEstadosMisiones()}");
		return cs.executeQuery();
	}

	public ResultSet listarEstadosTripulante() throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ListarEstadosTripulantes()}");
		return cs.executeQuery();
	}

	public ResultSet listarEstadosEvento() throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ListarEstadosEventos()}");
		return cs.executeQuery();
	}

	// --- REGISTROS (LOGS) ---
	public void registrarLog(int usuarioID, int accionID, int tipoEntidadID, int entidadID) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ARegistro(?, ?, ?, ?, ?)}");
		cs.setInt(1, usuarioID);
		cs.setInt(2, accionID);
		cs.setInt(3, tipoEntidadID);
		cs.setInt(4, entidadID);
		cs.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
		cs.execute();
	}

	public int obtenerUsuarioID(String usuario) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ConsultarUsuario(?)}");
		cs.setString(1, usuario);
		ResultSet rs = cs.executeQuery();
		if (rs.next()) return rs.getInt("UsuarioID");
		throw new SQLException("Usuario no encontrado: " + usuario);
	}

	// --- USUARIOS ---
	public void registrarUsuario(int rolID, String usuario, String nombre, String apellido, String clave) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL AUsuario(?, ?, ?, ?, ?)}");
		cs.setInt(1, rolID);
		cs.setString(2, usuario);
		cs.setString(3, nombre);
		cs.setString(4, apellido);
		cs.setString(5, clave);
		cs.execute();
	}

	public void modificarUsuario(int usuarioID, int rolID, String usuario, String nombre, String apellido, String clave) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL MUsuario(?, ?, ?, ?, ?, ?)}");
		cs.setInt(1, usuarioID);
		cs.setInt(2, rolID);
		cs.setString(3, usuario);
		cs.setString(4, nombre);
		cs.setString(5, apellido);
		cs.setString(6, clave);
		cs.execute();
	}

	public void bajaUsuario(String nombreUsuario) throws SQLException {
		int usuarioID = obtenerUsuarioID(nombreUsuario);
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL BUsuario(?)}");
		cs.setInt(1, usuarioID);
		cs.execute();
	}

	public ResultSet listarUsuarios() throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ListarUsuarios()}");
		return cs.executeQuery();
	}

	public String obtenerClaveUsuario(int usuarioID) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ConsultarUsuario(?)}");
		cs.setInt(1, usuarioID);
		ResultSet rs = cs.executeQuery();
		if (rs.next()) return rs.getString("Clave");
		return "";
	}

	// --- MISIONES ---
	public void registrarMision(int estadoMID, String nombre, String descripcion, Timestamp ini, Timestamp fin) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL AMision(?, ?, ?, ?, ?)}");
		cs.setInt(1, estadoMID);
		cs.setString(2, nombre);
		cs.setString(3, descripcion);
		cs.setTimestamp(4, ini);
		cs.setTimestamp(5, fin);
		cs.execute();
	}

	public void modificarMision(int id, int estadoMID, String nombre, String desc, Timestamp ini, Timestamp fin) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL MMision(?, ?, ?, ?, ?)}");
		cs.setInt(1, id);
		cs.setInt(2, estadoMID);
		cs.setString(3, nombre);
		cs.setString(4, desc);
		cs.setTimestamp(5, ini);
		cs.setTimestamp(6, fin);
		cs.execute();
	}

	public void actualizarEstadoMision(int id, int estadoID) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ActualizarEstadoMision(?, ?)}");
		cs.setInt(1, id);
		cs.setInt(2, estadoID);
		cs.execute();
	}

	public ResultSet listarMisionesActivas() throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ListarMisionesActivas()}");
		return cs.executeQuery();
	}

	public ResultSet consultarMision(int id) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ConsultarMision(?)}");
		cs.setInt(1, id);
		return cs.executeQuery();
	}

	// --- TRIPULANTES ---
	public void registrarTripulante(int estadoTID, int sexoID, int peso, int altura, String nombre, String apellido, String imagen, Date fechaNacimiento) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ATripulante(?, ?, ?, ?, ?, ?, ?, ?)}");
		cs.setInt(1, estadoTID);
		cs.setInt(2, sexoID);
		cs.setInt(3, peso);
		cs.setInt(4, altura);
		cs.setString(5, nombre);
		cs.setString(6, apellido);
		cs.setString(7, imagen);
		cs.setDate(8, fechaNacimiento);
		cs.execute();
	}

	public void modificarTripulante(int tripulanteID, int estadoTID, int sexoID, int peso, int altura, String nombre, String apellido, String imagen, Date fechaNacimiento) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL MTripulante(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
		cs.setInt(1, tripulanteID);
		cs.setInt(2, estadoTID);
		cs.setInt(3, sexoID);
		cs.setInt(4, peso);
		cs.setInt(5, altura);
		cs.setString(6, nombre);
		cs.setString(7, apellido);
		cs.setString(8, imagen);
		cs.setDate(9, fechaNacimiento);
		cs.execute();
	}

	public void bajaTripulante(int tripulanteID) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL BTripulante(?)}");
		cs.setInt(1, tripulanteID);
		cs.execute();
	}

	public void asignarTripulante(int tripID, int misID, Timestamp fecha) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL AGrupoMision(?, ?, ?)}");
		cs.setInt(1, tripID);
		cs.setInt(2, misID);
		cs.setTimestamp(3, fecha);
		cs.execute();
	}

	public ResultSet listarTripulantes() throws SQLException {
		return ConexionBD.getConexion().prepareCall("{CALL ListarTripulantes()}").executeQuery();
	}

	public ResultSet listarMisionesTripulante(int tripulanteID) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ListarMisionesTripulante(?)}");
		cs.setInt(1, tripulanteID);
		return cs.executeQuery();
	}

	public ResultSet consultarTripulante(int tripulanteID) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ConsultarTripulante(?)}");
		cs.setInt(1, tripulanteID);
		return cs.executeQuery();
	}

	// --- EVENTOS Y LOGS ---
	public void registrarEvento(int misionID, String titulo, String desc, Timestamp fecha) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL AEvento(?, ?, ?, ?)}");
		cs.setInt(1, misionID);
		cs.setString(2, titulo);
		cs.setString(3, desc);
		cs.setTimestamp(4, fecha);
		cs.execute();
	}

	public void bajaEvento(int eventoID) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL BEvento(?, ?)}");
		cs.setInt(1, eventoID);
		cs.setInt(2, 2);
		cs.execute();
	}

	public ResultSet consultarEventos(int misionID) throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ConsultarEventos(?)}");
		cs.setInt(1, misionID);
		return cs.executeQuery();
	}

	public ResultSet verLogs() throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL VerLogs()}");
		return cs.executeQuery();
	}
}
