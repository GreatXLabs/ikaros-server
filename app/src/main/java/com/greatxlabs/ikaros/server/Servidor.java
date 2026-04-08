package com.greatxlabs.ikaros.server;

import java.net.*;
import java.io.*;

public class Servidor {
    public static void main(String[] args) {
        int puerto = Configuracion.getPuerto();
        
        // Instanciar los componentes del sistema
        GestorSesiones gestorSesiones = new GestorSesiones();
        Protocolo protocolo = new Protocolo(gestorSesiones);
        
        System.out.println("Servidor Ikaros iniciado en puerto " + puerto);
        System.out.println("Esperando conexiones (Modo Secuencial)...");

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
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
        }
    }
}