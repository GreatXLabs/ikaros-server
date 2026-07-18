package com.greatxlabs.ikaros.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;

public class LogSistema {

    private static String archivoLog = "sistema.log";
    private static final Semaphore mutex = new Semaphore(1);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static void setArchivoLog(String path) {
        archivoLog = path;
    }

    public static void registrar(String mensaje) {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mutex.release();
            return;
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(archivoLog, true))) {
            writer.println("[" + LocalDateTime.now().format(FORMATO) + "] " + mensaje);
        } catch (IOException e) {
            System.err.println("Error escribiendo log: " + e.getMessage());
        } finally {
            mutex.release();
        }
    }
}
