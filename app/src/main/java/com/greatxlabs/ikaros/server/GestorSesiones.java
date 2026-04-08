package com.greatxlabs.ikaros.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona la autenticación y validación de tokens de sesión para el sistema Ikaros.
 * Actualmente utiliza datos predefinidos (hardcoded) para pruebas iniciales.
 */
public class GestorSesiones {
    
    // Almacena los tokens activos vinculados a un Rol
    private static final Map<String, String> sesionesActivas = new HashMap<>();

    /**
     * Valida las credenciales de un usuario y genera un token de sesión.
     * 
     * @param usuario Nombre de usuario.
     * @param clave Clave de acceso.
     * @return El token generado y el rol si es exitoso (formato token|ROL), o null si falla.
     */
    public String iniciarSesion(String usuario, String clave) {
        // Validación temporal hardcodeada
        if (usuario.equals("admin") && clave.equals("1234")) {
            String token = UUID.randomUUID().toString().substring(0, 8);
            String rol = "JEFE";
            sesionesActivas.put(token, rol);
            return token + "|" + rol;
        }
        return null;
    }

    /**
     * Verifica si un token es válido y está activo.
     * 
     * @param token El token a verificar.
     * @return true si el token existe, false en caso contrario.
     */
    public boolean esSesionValida(String token) {
        return sesionesActivas.containsKey(token);
    }

    /**
     * Obtiene el rol asociado a un token de sesión.
     * 
     * @param token El token del usuario.
     * @return El nombre del Rol o null si no existe.
     */
    public String obtenerRol(String token) {
        return sesionesActivas.get(token);
    }
}
