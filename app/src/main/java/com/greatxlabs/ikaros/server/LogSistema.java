package com.greatxlabs.ikaros.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;

/**
 * Log del sistema protegido por un mutex (semaforo binario).
 *
 * Todas las escrituras al archivo sistema.log pasan por el mutex
 * para evitar mezcla de lineas entre hilos concurrentes.
 */
public class LogSistema {

    private static final String ARCHIVO_LOG = "sistema.log";
    private static final Semaphore mutex = new Semaphore(1);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void registrar(String mensaje) {
        try {
            mutex.acquire();
            try (PrintWriter writer = new PrintWriter(new FileWriter(ARCHIVO_LOG, true))) {
                writer.println("[" + LocalDateTime.now().format(FORMATO) + "] " + mensaje);
            } catch (IOException e) {
                System.err.println("Error escribiendo log: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }
}
