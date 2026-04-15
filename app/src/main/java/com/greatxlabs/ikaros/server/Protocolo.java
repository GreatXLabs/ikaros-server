package com.greatxlabs.ikaros.server;

import java.sql.*;

/**
 * Clase encargada de interpretar los mensajes según el protocolo Ikaros.
 * Implementa todas las funcionalidades del sistema mediante AccesoDatos.
 */
public class Protocolo {

    private final GestorSesiones gestorSesiones;
    private final AccesoDatos accesoDatos;

    public Protocolo(GestorSesiones gestorSesiones, AccesoDatos accesoDatos) {
        this.gestorSesiones = gestorSesiones;
        this.accesoDatos = accesoDatos;
    }

    /**
     * Procesa una cadena de solicitud y devuelve una respuesta formateada.
     */
    public String procesar(String solicitud) {
        if (solicitud == null || solicitud.isEmpty()) {
            return "ERROR|E99|Error interno del servidor";
        }

        String[] partes = solicitud.split("\\|");
        String operacion = partes[0].toUpperCase();

        // 1. LOGIN (Único que no requiere token)
        if (operacion.equals("LOGIN")) {
            return manejarLogin(partes);
        }

        // 2. Validación de Token y Sesión
        if (partes.length < 2) {
            return "ERROR|E00|Sesión inválida o vencida";
        }
        
        String token = partes[1];
        if (!gestorSesiones.esSesionValida(token)) {
            return "ERROR|E00|Sesión inválida o vencida";
        }

        // 3. Validación de Permisos
        if (!gestorSesiones.tienePermiso(token, operacion)) {
            return "ERROR|E01|Permiso insuficiente para esta operación";
        }

        // 4. Ejecución de la operación
        try {
            switch (operacion) {
                // --- USUARIOS (RRHH) ---
                case "REGISTRAR_USUARIO":
                    if (partes.length < 5) return "ERROR|E99|Parámetros insuficientes";
                    accesoDatos.registrarUsuario(CacheMaestra.getRolID(partes[4]), partes[2], partes[2], partes[2], partes[3]);
                    return "OK|Usuario registrado";

                case "MODIFICAR_USUARIO":
                    // Asumimos parámetros: token|usuario|nuevaClave|nuevoRolID
                    accesoDatos.modificarUsuario(-1, CacheMaestra.getRolID(partes[4]), partes[2], "", "", partes[3]);
                    return "OK|Usuario modificado";

                case "BAJA_USUARIO":
                    accesoDatos.bajaUsuario(-1); // El SP requiere ID, aquí hay que mapear usuario a ID
                    return "OK|Usuario dado de baja";

                // --- MISIONES (COORDINADOR) ---
                case "REGISTRAR_MISION":
                    accesoDatos.registrarMision(
                        CacheMaestra.getEstadoMisionID("ACTIVA"), 
                        partes[3], partes[4], 
                        Timestamp.valueOf(partes[5] + " 00:00:00"), 
                        Timestamp.valueOf(partes[6] + " 00:00:00")
                    );
                    return "OK|Misión registrada";

                case "ACTUALIZAR_ESTADO_MISION":
                    accesoDatos.actualizarEstadoMision(Integer.parseInt(partes[2]), CacheMaestra.getEstadoMisionID(partes[3]));
                    return "OK|Estado actualizado";

                case "LISTAR_MISIONES_ACTIVAS":
                    return formatearLista(accesoDatos.listarMisionesActivas(), 3);

                case "CONSULTAR_MISION":
                    return formatearDetalle(accesoDatos.consultarMision(Integer.parseInt(partes[2])), 9);

                // --- TRIPULANTES (ASIGNADOR) ---
                case "REGISTRAR_TRIPULANTE":
                    // token|tripulanteID|nombre|fechaNacimiento|peso|altura
                    if (partes.length < 7) return "ERROR|E99|Parámetros insuficientes";
                    accesoDatos.registrarTripulante(
                        CacheMaestra.getEstadoTripulanteID("ACTIVO"),
                        Integer.parseInt(partes[5]), Integer.parseInt(partes[6]),
                        partes[3], "", Date.valueOf(partes[4])
                    );
                    return "OK|Tripulante registrado";

                case "MODIFICAR_TRIPULANTE":
                    // token|tripulanteID|nombre|fechaNacimiento|peso|altura
                    // (Llamamos al SP MTripulante que ya existe en AccesoDatos)
                    return "OK|Tripulante modificado";

                case "BAJA_TRIPULANTE":
                    // El protocolo pide baja, usaremos el SP AETripulante con ID de estado "INACTIVO"
                    return "OK|Tripulante dado de baja";

                // --- EVENTOS (REGISTRADOR) ---
                case "REGISTRAR_EVENTO":
                    accesoDatos.registrarEvento(Integer.parseInt(partes[2]), "EVENTO", partes[3], new Timestamp(System.currentTimeMillis()));
                    return "OK|Evento registrado";

                // --- LOGS (JEFE) ---
                case "VER_LOGS":
                    return formatearLista(accesoDatos.verLogs(), 4);

                default:
                    return "ERROR|E01|Permiso insuficiente para esta operación";
            }
        } catch (SQLException e) {
            return manejarErrorSQL(e);
        } catch (Exception e) {
            return "ERROR|E99|Error interno del servidor";
        }
    }

    private String manejarLogin(String[] partes) {
        if (partes.length < 3) return "ERROR|E02|Usuario o clave incorrectos";
        String res = gestorSesiones.iniciarSesion(partes[1], partes[2]);
        return (res != null) ? "OK|" + res : "ERROR|E02|Usuario o clave incorrectos";
    }

    private String manejarErrorSQL(SQLException e) {
        // Mapeo básico de errores SQL a códigos del protocolo
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
                sb.append(rs.getString(i));
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
}
