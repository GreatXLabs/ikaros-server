package com.greatxlabs.ikaros.server;

import java.net.*;
import java.io.*;

/**
 * Punto de entrada del servidor Ikaros.
 *
 * Modo CONCURRENTE: el hilo principal acepta conexiones y lanza
 * un hilo nuevo por cada cliente. Cada hilo ejecuta su propio
 * loop de lectura/procesamiento/escritura con su instancia de Protocolo.
 */
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
            try (socket) {
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);

                Protocolo protocolo = new Protocolo(gestorSesiones, accesoDatos);

                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    System.out.println("[" + Thread.currentThread().getName() + "] Solicitud: " + mensaje);

                    String respuesta = protocolo.procesar(mensaje);

                    System.out.println("[" + Thread.currentThread().getName() + "] Respuesta: " + respuesta);
                    salida.println(respuesta);
                }
            } catch (IOException e) {
                System.err.println("[" + Thread.currentThread().getName() + "] Error manejando cliente: " + e.getMessage());
            } finally {
                System.out.println("[" + Thread.currentThread().getName() + "] Cliente desconectado.");
                LogSistema.registrar("DESCONEXION " + socket.getInetAddress());
            }
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
            Runtime.getRuntime().addShutdownHook(new Thread(ConexionBD::cerrarConexion));

            while (true) {
                Socket cliente = serverSocket.accept();
                System.out.println("Cliente conectado: " + cliente.getInetAddress());
                LogSistema.registrar("CONEXION nueva desde " + cliente.getInetAddress());

                Thread hilo = new Thread(new ManejadorCliente(cliente, gestorSesiones, accesoDatos));
                hilo.start();
            }
        } catch (IOException e) {
            System.err.println("Error crítico en el servidor: " + e.getMessage());
        } finally {
            ConexionBD.cerrarConexion();
        }
    }
}
