package com.greatxlabs.ikaros.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona la autenticación y validación de tokens de sesión para el sistema Ikaros.
 * Implementa vencimiento por inactividad de 30 minutos.
 */
public class GestorSesiones {
    
    /**
     * Representa una sesión activa en el sistema con su tiempo de última actividad.
     */
    private static class Sesion {
        String rol;
        long ultimaActividad;

        Sesion(String rol) {
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

    // Almacena los tokens activos vinculados a una Sesión
    private static final Map<String, Sesion> sesionesActivas = new HashMap<>();
    private final AccesoDatos accesoDatos;

    public GestorSesiones(AccesoDatos accesoDatos) {
        this.accesoDatos = accesoDatos;
    }

    /**
     * Valida las credenciales de un usuario y genera un token de sesión.
     * 
     * @param usuario Nombre de usuario.
     * @param clave Clave de acceso.
     * @return El token generado y el rol si es exitoso (formato token|ROL), o null si falla.
     */
    public String iniciarSesion(String usuario, String clave) {
        try (java.sql.ResultSet rs = accesoDatos.validarLogin(usuario, clave)) {
            if (rs.next()) {
                String token = UUID.randomUUID().toString().substring(0, 8);
                String rol = rs.getString("NombreRol");
                
                sesionesActivas.put(token, new Sesion(rol));
                return token + "|" + rol;
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error en validación de sesión: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifica si un rol tiene permiso para ejecutar una operación.
     * 
     * @param token El token del usuario.
     * @param operacion El nombre de la operación (ej: REGISTRAR_MISION).
     * @return true si tiene permiso, false en caso contrario.
     */
    public boolean tienePermiso(String token, String operacion) {
        Sesion sesion = sesionesActivas.get(token);
        if (sesion == null) return false;

        // Si la sesión expiró, la eliminamos y denegamos el permiso
        if (sesion.haExpirado()) {
            sesionesActivas.remove(token);
            return false;
        }

        String rol = sesion.rol;
        // El JEFE puede hacer todo
        if (rol.equals("JEFE")) return true;

        switch (rol) {
            case "RRHH":
                return operacion.equals("REGISTRAR_USUARIO") || 
                       operacion.equals("MODIFICAR_USUARIO") || 
                       operacion.equals("BAJA_USUARIO");
            
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
                       operacion.equals("CONSULTAR_TRIPULANTE");

            case "REGISTRADOR":
                return operacion.equals("REGISTRAR_EVENTO") || 
                       operacion.equals("BAJA_EVENTO") || 
                       operacion.equals("CONSULTAR_EVENTOS");
            
            default:
                return false;
        }
    }

    /**
     * Verifica si un token es válido, está activo y no ha expirado.
     * Si es válido, renueva el tiempo de actividad.
     * 
     * @param token El token a verificar.
     * @return true si la sesión es válida y está activa.
     */
    public boolean esSesionValida(String token) {
        Sesion sesion = sesionesActivas.get(token);
        if (sesion == null) return false;

        if (sesion.haExpirado()) {
            sesionesActivas.remove(token);
            return false;
        }

        // Renovación automática con cada uso exitoso
        sesion.renovar();
        return true;
    }

    /**
     * Obtiene el rol asociado a un token de sesión.
     * 
     * @param token El token del usuario.
     * @return El nombre del Rol o null si no existe o expiró.
     */
    public String obtenerRol(String token) {
        Sesion sesion = sesionesActivas.get(token);
        if (sesion != null && !sesion.haExpirado()) {
            return sesion.rol;
        }
        return null;
    }
}
