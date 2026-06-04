package com.greatxlabs.ikaros.server;

import java.net.*;
import java.io.*;

/**
 * Punto de entrada del servidor Ikaros.
 *
 * Modo SECUENCIAL: acepta un cliente a la vez. El servidor se bloquea
 * en serverSocket.accept() hasta que llega una conexion, y no acepta
 * la siguiente hasta que el cliente actual se desconecta.
 *
 * Para Bimestre 2: debera convertirse a servidor concurrente
 * con un thread por conexion (o thread pool).
 */
public class Servidor {
    public static void main(String[] args) {
        int puerto = Configuracion.getPuerto();
        
        // Instanciar componentes de datos
        AccesoDatos accesoDatos = new AccesoDatos();
        CacheMaestra cache = new CacheMaestra(accesoDatos);
        
        // Inicialización inicial
        cache.cargarTodo();
        
        // Instanciar los componentes de lógica
        GestorSesiones gestorSesiones = new GestorSesiones(accesoDatos);
        Protocolo protocolo = new Protocolo(gestorSesiones, accesoDatos);
        
        System.out.println("Servidor Ikaros iniciado en puerto " + puerto);
        System.out.println("Esperando conexiones (Modo Secuencial)...");

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            // Gancho para cerrar la conexión limpiamente al apagar el servidor
            Runtime.getRuntime().addShutdownHook(new Thread(ConexionBD::cerrarConexion));
            
            while (true) {
                // El servidor se detiene aquí hasta que llega un cliente
                try (Socket cliente = serverSocket.accept()) {
                    System.out.println("Cliente conectado: " + cliente.getInetAddress());
                    
                    BufferedReader entrada = new BufferedReader(
                            new InputStreamReader(cliente.getInputStream()));
                    PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);
                    
                    String mensaje;
                    // Bucle para procesar múltiples mensajes de un mismo cliente
                    while ((mensaje = entrada.readLine()) != null) {
                        System.out.println("Solicitud: " + mensaje);
                        
                        // Delegar el procesamiento al protocolo
                        String respuesta = protocolo.procesar(mensaje);
                        
                        System.out.println("Respuesta: " + respuesta);
                        salida.println(respuesta);
                    }
                    
                    System.out.println("Cliente desconectado.");
                } catch (IOException e) {
                    System.err.println("Error manejando cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error crítico en el servidor: " + e.getMessage());
        } finally {
            ConexionBD.cerrarConexion();
        }
    }
}