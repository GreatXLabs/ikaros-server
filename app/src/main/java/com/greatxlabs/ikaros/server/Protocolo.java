package com.greatxlabs.ikaros.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public String procesar(String solicitud) {
        if (solicitud == null || solicitud.isEmpty()) {
            return "ERROR|E99|Solicitud vacía";
        }

        String[] partes = solicitud.split("\\|");
        String operacion = partes[0].toUpperCase();

        // 1. LOGIN (No requiere token)
        if (operacion.equals("LOGIN")) {
            return manejarLogin(partes);
        }

        // 2. Validación de Token y Permisos
        if (partes.length < 2) return "ERROR|E00|Token requerido";
        String token = partes[1];

        if (!gestorSesiones.esSesionValida(token)) {
            return "ERROR|E00|Sesión inválida o vencida";
        }

        if (!gestorSesiones.tienePermiso(token, operacion)) {
            return "ERROR|E01|Permiso insuficiente";
        }

        // 3. Ejecución de la operación
        try {
            switch (operacion) {
                // --- USUARIOS ---
                case "REGISTRAR_USUARIO":
                    accesoDatos.registrarUsuario(CacheMaestra.getRolID(partes[4]), partes[2], partes[2], partes[2], partes[3]);
                    return "OK|Usuario registrado";

                // --- MISIONES ---
                case "REGISTRAR_MISION":
                    accesoDatos.registrarMision(
                        CacheMaestra.getEstadoMisionID("ACTIVA"), 
                        partes[3], partes[4], 
                        Timestamp.valueOf(partes[5] + " 00:00:00"), 
                        Timestamp.valueOf(partes[6] + " 00:00:00")
                    );
                    return "OK|Misión registrada";

                case "LISTAR_MISIONES_ACTIVAS":
                    return formatearLista(accesoDatos.listarMisionesActivas(), 3); // id:nombre:estado

                case "ACTUALIZAR_ESTADO_MISION":
                    accesoDatos.actualizarEstadoMision(Integer.parseInt(partes[2]), CacheMaestra.getEstadoMisionID(partes[3]));
                    return "OK|Estado actualizado";

                // --- TRIPULANTES ---
                case "REGISTRAR_TRIPULANTE":
                    accesoDatos.registrarTripulante(
                        CacheMaestra.getEstadoTripulanteID("ACTIVO"),
                        Integer.parseInt(partes[4]), Integer.parseInt(partes[5]),
                        partes[3], "", Date.valueOf(partes[4])
                    );
                    return "OK|Tripulante registrado";

                case "LISTAR_TRIPULANTES":
                    return formatearLista(accesoDatos.listarTripulantes(), 3); // id:nombre:estado

                case "ASIGNAR_TRIPULANTE":
                    accesoDatos.asignarTripulante(Integer.parseInt(partes[2]), Integer.parseInt(partes[3]), new Timestamp(System.currentTimeMillis()));
                    return "OK|Tripulante asignado";

                // --- EVENTOS Y LOGS ---
                case "REGISTRAR_EVENTO":
                    accesoDatos.registrarEvento(Integer.parseInt(partes[2]), "EVENTO", partes[3], new Timestamp(System.currentTimeMillis()));
                    return "OK|Evento registrado";

                case "VER_LOGS":
                    return formatearLista(accesoDatos.verLogs(), 4); // id:usuario:accion:fecha

                default:
                    return "ERROR|E01|Operación no implementada en esta fase";
            }
        } catch (Exception e) {
            System.err.println("Error ejecutando " + operacion + ": " + e.getMessage());
            return "ERROR|E99|Error interno: " + e.getMessage();
        }
    }

    private String manejarLogin(String[] partes) {
        if (partes.length < 3) return "ERROR|E02|Faltan credenciales";
        String res = gestorSesiones.iniciarSesion(partes[1], partes[2]);
        return (res != null) ? "OK|" + res : "ERROR|E02|Credenciales incorrectas";
    }

    /**
     * Convierte un ResultSet en una cadena formateada: id:campo2:campo3;id2:campo2:campo3
     */
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
}
