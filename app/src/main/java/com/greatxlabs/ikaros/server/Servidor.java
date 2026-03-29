package com.greatxlabs.ikaros.server;

import java.net.*;
import java.io.*;

public class Servidor {
    public static void main(String[] args) {
        int puerto = Configuracion.getPuerto();
        int maxClientes = Configuracion.getMaxClientes();
        
        System.out.println("Iniciando servidor en puerto " + puerto +
                           " (máx. clientes: " + maxClientes + ")");

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Esperando clientes...");

            while (true) {
                try (Socket cliente = serverSocket.accept()) {
                    System.out.println("Cliente conectado: " + cliente.getInetAddress());
                    
                    BufferedReader entrada = new BufferedReader(
                            new InputStreamReader(cliente.getInputStream()));
                    PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);
                    
                    String mensaje = entrada.readLine();
                    System.out.println("Recibido: " + mensaje);
                    salida.println("OK|" + mensaje);
                }
            }

        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
}