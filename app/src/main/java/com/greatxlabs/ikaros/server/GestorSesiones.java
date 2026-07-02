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
 * El mapa sesionesActivas esta protegido por un SemaforoRW.
 *
 * Roles: JEFE (acceso total), RRHH, COORDINADOR, ASIGNADOR, REGISTRADOR.
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
    private static final SemaforoRW semSesiones = new SemaforoRW();

    private final AccesoDatos accesoDatos;

    public GestorSesiones(AccesoDatos accesoDatos) {
        this.accesoDatos = accesoDatos;
    }

    public String iniciarSesion(String usuario, String clave) {
        if (accesoDatos == null) return null;
        try {
            boolean credencialesValidas = accesoDatos.validarLogin(usuario, clave); //CAMBIADO: ahora valida contra JSON
            if (!credencialesValidas) return null;

            AccesoDatos.UsuarioLoginResult datos = accesoDatos.obtenerDatosUsuarioParaLogin(usuario); //CAMBIADO: obtiene datos desde JSON
            if (datos == null) return null;

            String token = UUID.randomUUID().toString().substring(0, 8);
            int usuarioID = datos.usuarioID;
            String rol = datos.rol; // ya viene del JSON

            semSesiones.iniciarEscritura();
            try {
                sesionesActivas.put(token, new Sesion(usuarioID, rol.toUpperCase()));
            } finally {
                semSesiones.terminarEscritura();
            }
            return token + "|" + rol.toUpperCase() + "|" + usuarioID;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public boolean tienePermiso(String token, String operacion) {
        try {
            semSesiones.iniciarLectura();
            Sesion sesion;
            try {
                sesion = sesionesActivas.get(token);
            } finally {
                semSesiones.terminarLectura();
            }

            if (sesion == null) return false;

            if (sesion.haExpirado()) {
                semSesiones.iniciarEscritura();
                try {
                    sesionesActivas.remove(token);
                } finally {
                    semSesiones.terminarEscritura();
                }
                return false;
            }

            if (operacion.equals("REGISTRAR_LOG")) return true;

            String rol = sesion.rol;
            if (rol.equals("JEFE")) return true;

            Set<String> permitidas = PERMISOS_POR_ROL.get(rol);
            return permitidas != null && permitidas.contains(operacion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean esSesionValida(String token) {
        try {
            semSesiones.iniciarLectura();
            Sesion sesion;
            try {
                sesion = sesionesActivas.get(token);
            } finally {
                semSesiones.terminarLectura();
            }

            if (sesion == null) return false;

            if (sesion.haExpirado()) {
                semSesiones.iniciarEscritura();
                try {
                    sesionesActivas.remove(token);
                } finally {
                    semSesiones.terminarEscritura();
                }
                return false;
            }

            semSesiones.iniciarEscritura();
            try {
                sesion.renovar();
            } finally {
                semSesiones.terminarEscritura();
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public String obtenerRol(String token) {
        try {
            semSesiones.iniciarLectura();
            try {
                Sesion sesion = sesionesActivas.get(token);
                if (sesion != null && !sesion.haExpirado()) {
                    return sesion.rol;
                }
                return null;
            } finally {
                semSesiones.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public Integer obtenerUsuarioID(String token) {
        try {
            semSesiones.iniciarLectura();
            try {
                Sesion sesion = sesionesActivas.get(token);
                if (sesion != null && !sesion.haExpirado()) {
                    return sesion.usuarioID;
                }
                return null;
            } finally {
                semSesiones.terminarLectura();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}