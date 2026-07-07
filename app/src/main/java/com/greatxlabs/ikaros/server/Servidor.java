package com.greatxlabs.ikaros.server;

import java.net.*;
import java.io.*;

public class Servidor {

    private static class ManejadorCliente implements Runnable {
        private final Socket socket;
        private final GestorSesiones gestorSesiones;
        private final AccesoDatos accesoDatos;

        ManejadorCliente(Socket socket, GestorSesiones gestorSesiones, AccesoDatos accesoDatos) {
            this.socket = socket;
            this.gestorSesiones = gestorSesiones;
            this.accesoDatos = accesoDatos;
        }

        @Override
        public void run() {
            String direccionCliente = socket.getInetAddress().toString();
            try (socket) {
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);

                Protocolo protocolo = new Protocolo(gestorSesiones, accesoDatos);

                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    System.out.println("[" + Thread.currentThread().getName() + "] Solicitud: " + enmascararClave(mensaje));

                    String respuesta = protocolo.procesar(mensaje);

                    System.out.println("[" + Thread.currentThread().getName() + "] Respuesta: " + respuesta);
                    salida.println(respuesta);
                }
            } catch (IOException e) {
                System.err.println("[" + Thread.currentThread().getName() + "] Error manejando cliente: " + e.getMessage());
            } finally {
                System.out.println("[" + Thread.currentThread().getName() + "] Cliente desconectado.");
                LogSistema.registrar("DESCONEXION " + direccionCliente);
            }
        }

        private static String enmascararClave(String mensaje) {
            String[] partes = mensaje.split("\\|", -1);
            if (partes.length == 0) return mensaje;

            int indiceClave;
            switch (partes[0]) {
                case "LOGIN": indiceClave = 2; break;
                case "REGISTRAR_USUARIO": indiceClave = 3; break;
                case "MODIFICAR_USUARIO": indiceClave = 4; break;
                default: return mensaje;
            }

            if (indiceClave >= partes.length) return mensaje;
            partes[indiceClave] = "***";
            return String.join("|", partes);
        }
    }

    public static void main(String[] args) {
        int puerto = Configuracion.getPuerto();

        AccesoDatos accesoDatos = new AccesoDatos();
        CacheMaestra cache = new CacheMaestra(accesoDatos);

        cache.cargarTodo();

        GestorSesiones gestorSesiones = new GestorSesiones(accesoDatos);

        System.out.println("Servidor Ikaros iniciado en puerto " + puerto);
        System.out.println("Esperando conexiones (Modo Concurrente)...");

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            while (true) {
                Socket cliente = serverSocket.accept();
                System.out.println("Cliente conectado: " + cliente.getInetAddress());
                LogSistema.registrar("CONEXION nueva desde " + cliente.getInetAddress());

                Thread hilo = new Thread(new ManejadorCliente(cliente, gestorSesiones, accesoDatos));
                hilo.start();
            }
        } catch (IOException e) {
            System.err.println("Error crítico en el servidor: " + e.getMessage());
        }
    }
}
