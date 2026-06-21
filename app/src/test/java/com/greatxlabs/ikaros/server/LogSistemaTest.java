package com.greatxlabs.ikaros.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class LogSistemaTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void configurarArchivoLog() {
        LogSistema.setArchivoLog(tempDir.resolve("test.log").toString());
    }

    @AfterEach
    void restaurarArchivoLog() {
        LogSistema.setArchivoLog("sistema.log");
    }

    @Test
    void creaArchivoSiNoExiste() throws Exception {
        Path logPath = tempDir.resolve("test.log");
        assertFalse(Files.exists(logPath));

        LogSistema.registrar("test");

        assertTrue(Files.exists(logPath));
        List<String> lineas = Files.readAllLines(logPath);
        assertEquals(1, lineas.size());
        assertTrue(lineas.get(0).contains("test"));
    }

    @Test
    void escriturasConcurrentesNoInterleavan() throws Exception {
        int numHilos = 20;
        int mensajesPorHilo = 50;
        CountDownLatch barrera = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(numHilos);

        for (int i = 0; i < numHilos; i++) {
            final int hiloID = i;
            new Thread(() -> {
                try {
                    barrera.await();
                    for (int j = 0; j < mensajesPorHilo; j++) {
                        LogSistema.registrar("HILO-" + hiloID + " mensaje-" + j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fin.countDown();
                }
            }).start();
        }

        barrera.countDown();
        assertTrue(fin.await(30, TimeUnit.SECONDS));

        Path logPath = tempDir.resolve("test.log");
        List<String> lineas = Files.readAllLines(logPath);

        assertEquals(numHilos * mensajesPorHilo, lineas.size(),
            "Deberia haber exactamente " + (numHilos * mensajesPorHilo) + " lineas");

        for (String linea : lineas) {
            assertTrue(linea.startsWith("["),
                "Cada linea debe empezar con timestamp: " + linea);
            assertTrue(linea.contains("HILO-"),
                "Cada linea debe contener un mensaje completo: " + linea);
        }
    }

    @Test
    void formatoTimestampCorrecto() throws Exception {
        LogSistema.registrar("prueba formato");

        Path logPath = tempDir.resolve("test.log");
        List<String> lineas = Files.readAllLines(logPath);
        assertEquals(1, lineas.size());
        assertTrue(lineas.get(0).matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\] prueba formato"),
            "Formato incorrecto: " + lineas.get(0));
    }

    @Test
    void appendNoSobreescribe() throws Exception {
        LogSistema.registrar("linea 1");
        LogSistema.registrar("linea 2");
        LogSistema.registrar("linea 3");

        Path logPath = tempDir.resolve("test.log");
        List<String> lineas = Files.readAllLines(logPath);
        assertEquals(3, lineas.size());
        assertTrue(lineas.get(0).contains("linea 1"));
        assertTrue(lineas.get(2).contains("linea 3"));
    }
}
