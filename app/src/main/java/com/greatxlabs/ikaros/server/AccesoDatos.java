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
		PreparedStatement ps = con.prepareStatement(
			"SELECT U.UsuarioID, U.Usuario, U.Nombre, U.Apellido, U.Clave, R.Rol AS NombreRol " +
			"FROM Usuarios U INNER JOIN Roles R ON U.RolID = R.RolID " +
			"WHERE U.Usuario = ?"
		);
		ps.setString(1, usuario);
		return ps.executeQuery();
	}

	public ResultSet obtenerTablaMaestra(String tipoTabla) throws SQLException {
		Connection con = ConexionBD.getConexion();
		String tabla;
		switch (tipoTabla) {
		case "Roles": tabla = "Roles"; break;
		case "EstadosMisiones": tabla = "EstadosMisiones"; break;
		case "EstadosTripulantes": tabla = "EstadosTripulantes"; break;
		case "EstadosEventos": tabla = "EstadosEventos"; break;
		case "Aptitudes": tabla = "Aptitudes"; break;
		default: throw new SQLException("Tabla maestra desconocida: " + tipoTabla);
		}
		PreparedStatement ps = con.prepareStatement("SELECT * FROM " + tabla);
		return ps.executeQuery();
	}

	// --- ROLES ---
	public ResultSet consultarRoles() throws SQLException {
		Connection con = ConexionBD.getConexion();
		CallableStatement cs = con.prepareCall("{CALL ConsultarRoles()}");
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
		PreparedStatement ps = con.prepareStatement("SELECT UsuarioID FROM Usuarios WHERE Usuario = ?");
		ps.setString(1, usuario);
		ResultSet rs = ps.executeQuery();
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
		PreparedStatement ps = con.prepareStatement("DELETE FROM Usuarios WHERE UsuarioID = ?");
		ps.setInt(1, usuarioID);
		ps.execute();
	}

	public ResultSet listarUsuarios() throws SQLException {
		Connection con = ConexionBD.getConexion();
		PreparedStatement ps = con.prepareStatement(
			"SELECT U.UsuarioID, U.Usuario, U.Nombre, U.Apellido, R.Rol AS NombreRol " +
			"FROM Usuarios U INNER JOIN Roles R ON U.RolID = R.RolID"
		);
		return ps.executeQuery();
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
		PreparedStatement ps = con.prepareStatement("UPDATE Misiones SET EstadoMID = ? WHERE MisionID = ?");
		ps.setInt(1, estadoID);
		ps.setInt(2, id);
		ps.execute();
	}

	public ResultSet listarMisionesActivas() throws SQLException {
		Connection con = ConexionBD.getConexion();
		PreparedStatement ps = con.prepareStatement(
			"SELECT M.MisionID, M.Nombre, E.Estado " +
			"FROM Misiones M INNER JOIN EstadosMisiones E ON M.EstadoMID = E.EstadoMID " +
			"WHERE M.EstadoMID NOT IN (4, 5) " +
			"ORDER BY M.MisionID"
		);
		return ps.executeQuery();
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
		CallableStatement cs = con.prepareCall("{CALL MTripulante(?, ?, ?, ?, ?, ?, ?, ?)}");
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
		PreparedStatement ps = con.prepareStatement(
			"UPDATE Tripulantes SET EstadoTID = 2 WHERE TripulanteID = ?"
		);
		ps.setInt(1, tripulanteID);
		ps.execute();
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
		PreparedStatement ps = con.prepareStatement(
			"SELECT R.RegistroID, U.Usuario, R2.Rol, A.Accion, E.TipoEntidad, R.EntidadID, R.FechaHora " +
			"FROM Registros R " +
			"INNER JOIN Usuarios U ON R.UsuarioID = U.UsuarioID " +
			"INNER JOIN Roles R2 ON U.RolID = R2.RolID " +
			"INNER JOIN Acciones A ON R.AccionID = A.AccionID " +
			"INNER JOIN Entidades E ON R.TipoEntidadID = E.TipoEntidadID " +
			"ORDER BY R.FechaHora DESC"
		);
		return ps.executeQuery();
	}
}
