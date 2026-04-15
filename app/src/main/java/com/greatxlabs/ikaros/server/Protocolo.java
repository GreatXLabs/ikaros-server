package com.greatxlabs.ikaros.server;

/**
 * Clase encargada de interpretar los mensajes según el protocolo Ikaros.
 * Recibe cadenas de texto y devuelve la respuesta correspondiente.
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
     * 
     * @param solicitud El mensaje crudo recibido del cliente.
     * @return El mensaje de respuesta según el protocolo (OK o ERROR).
     */
    public String procesar(String solicitud) {
        if (solicitud == null || solicitud.isEmpty()) {
            return "ERROR|E99|Solicitud vacía";
        }

        // Dividir los campos por el separador |
        String[] partes = solicitud.split("\\|");
        String operacion = partes[0].toUpperCase();

        // 1. Caso especial: LOGIN (único que no requiere token)
        if (operacion.equals("LOGIN")) {
            return manejarLogin(partes);
        }

        // 2. Para el resto de operaciones, el segundo parámetro debe ser el TOKEN
        if (partes.length < 2) {
            return "ERROR|E00|Token requerido";
        }

        String token = partes[1];
        if (!gestorSesiones.esSesionValida(token)) {
            return "ERROR|E00|Sesión inválida o vencida";
        }

        // 3. Selección de la lógica según la operación
        switch (operacion) {
            case "REGISTRAR_MISION":
                return "OK|Misión registrada (Hardcoded)";
                
            case "LISTAR_TRIPULANTES":
                return "OK|T01:Emiliano Ruiz:ACTIVO;T02:Ana Perez:ACTIVO";
                
            case "CONSULTAR_MISION":
                return "OK|M01|Apolo XII|Mision hardcoded|ACTIVA|2026-06-01|2026-09-30||";

            default:
                return "ERROR|E01|Operación desconocida o permiso insuficiente";
        }
    }

    /**
     * Lógica interna para manejar el inicio de sesión.
     */
    private String manejarLogin(String[] partes) {
        if (partes.length < 3) {
            return "ERROR|E02|Usuario o clave faltante";
        }
        
        String usuario = partes[1];
        String clave = partes[2];
        String resultado = gestorSesiones.iniciarSesion(usuario, clave);
        
        if (resultado != null) {
            return "OK|" + resultado;
        } else {
            return "ERROR|E02|Usuario o clave incorrectos";
        }
    }
}
