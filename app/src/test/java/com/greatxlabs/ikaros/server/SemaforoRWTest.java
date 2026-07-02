package com.greatxlabs.ikaros.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SemaforoRWTest {

    @Test
    void lectoresConcurrentesNoSeBloquean() throws Exception {
        SemaforoRW sem = new SemaforoRW();
        int numLectores = 10;
        CountDownLatch todosLeyendo = new CountDownLatch(numLectores);
        CountDownLatch terminar = new CountDownLatch(1);
        AtomicInteger lectoresSimultaneos = new AtomicInteger(0);
        AtomicInteger maxSimultaneos = new AtomicInteger(0);

        for (int i = 0; i < numLectores; i++) {
            new Thread(() -> {
                try {
                    sem.iniciarLectura();
                    int actual = lectoresSimultaneos.incrementAndGet();
                    maxSimultaneos.updateAndGet(max -> Math.max(max, actual));
                    todosLeyendo.countDown();
                    terminar.await();
                    lectoresSimultaneos.decrementAndGet();
                    sem.terminarLectura();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        assertTrue(todosLeyendo.await(5, TimeUnit.SECONDS),
            "Todos los lectores deberian poder leer al mismo tiempo");
        assertTrue(maxSimultaneos.get() > 1,
            "Deberia haber mas de un lector simultaneo, hubo: " + maxSimultaneos.get());
        terminar.countDown();
    }

    @Test
    void escrituraExcluyeLectores() throws Exception {
        SemaforoRW sem = new SemaforoRW();
        AtomicBoolean escritorActivo = new AtomicBoolean(false);
        AtomicBoolean violacion = new AtomicBoolean(false);
        CountDownLatch escritorInicio = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(2);

        new Thread(() -> {
            try {
                sem.iniciarEscritura();
                escritorActivo.set(true);
                escritorInicio.countDown();
                Thread.sleep(200);
                escritorActivo.set(false);
                sem.terminarEscritura();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fin.countDown();
            }
        }).start();

        escritorInicio.await();
        Thread.sleep(50);

        new Thread(() -> {
            try {
                sem.iniciarLectura();
                if (escritorActivo.get()) {
                    violacion.set(true);
                }
                sem.terminarLectura();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fin.countDown();
            }
        }).start();

        assertTrue(fin.await(5, TimeUnit.SECONDS));
        assertFalse(violacion.get(), "El lector no deberia entrar mientras el escritor esta activo");
    }

    @Test
    void escrituraExcluyeOtrosEscritores() throws Exception {
        SemaforoRW sem = new SemaforoRW();
        AtomicInteger escritoresActivos = new AtomicInteger(0);
        AtomicBoolean violacion = new AtomicBoolean(false);
        CountDownLatch fin = new CountDownLatch(2);

        Runnable escritor = () -> {
            try {
                sem.iniciarEscritura();
                int activos = escritoresActivos.incrementAndGet();
                if (activos > 1) violacion.set(true);
                Thread.sleep(100);
                escritoresActivos.decrementAndGet();
                sem.terminarEscritura();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fin.countDown();
            }
        };

        new Thread(escritor).start();
        new Thread(escritor).start();

        assertTrue(fin.await(5, TimeUnit.SECONDS));
        assertFalse(violacion.get(), "Nunca deberia haber mas de un escritor activo");
    }

    @RepeatedTest(5)
    void sinStarvationDeEscritores() throws Exception {
        SemaforoRW sem = new SemaforoRW();
        AtomicBoolean escritorTermino = new AtomicBoolean(false);
        CountDownLatch lectorInicial = new CountDownLatch(1);
        CountDownLatch escritorEsperando = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(1);

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    sem.iniciarLectura();
                    lectorInicial.countDown();
                    Thread.sleep(50);
                    sem.terminarLectura();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        lectorInicial.await();

        new Thread(() -> {
            try {
                escritorEsperando.countDown();
                sem.iniciarEscritura();
                escritorTermino.set(true);
                sem.terminarEscritura();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fin.countDown();
            }
        }).start();

        escritorEsperando.await();
        Thread.sleep(20);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    sem.iniciarLectura();
                    Thread.sleep(10);
                    sem.terminarLectura();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        assertTrue(fin.await(5, TimeUnit.SECONDS),
            "El escritor deberia terminar, no quedar en starvation");
        assertTrue(escritorTermino.get());
    }

    @RepeatedTest(3)
    void estresLectoresYEscritores() throws Exception {
        SemaforoRW sem = new SemaforoRW();
        int numHilos = 20;
        AtomicInteger lectoresActivos = new AtomicInteger(0);
        AtomicInteger escritoresActivos = new AtomicInteger(0);
        AtomicBoolean violacion = new AtomicBoolean(false);
        CountDownLatch barrera = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(numHilos);

        for (int i = 0; i < numHilos; i++) {
            boolean esEscritor = (i % 4 == 0);
            new Thread(() -> {
                try {
                    barrera.await();
                    for (int j = 0; j < 50; j++) {
                        if (esEscritor) {
                            sem.iniciarEscritura();
                            int e = escritoresActivos.incrementAndGet();
                            int l = lectoresActivos.get();
                            if (e > 1 || l > 0) violacion.set(true);
                            escritoresActivos.decrementAndGet();
                            sem.terminarEscritura();
                        } else {
                            sem.iniciarLectura();
                            int e = escritoresActivos.get();
                            lectoresActivos.incrementAndGet();
                            if (e > 0) violacion.set(true);
                            lectoresActivos.decrementAndGet();
                            sem.terminarLectura();
                        }
                    }
                } catch (Exception e) {
                    violacion.set(true);
                } finally {
                    fin.countDown();
                }
            }).start();
        }

        barrera.countDown();
        assertTrue(fin.await(30, TimeUnit.SECONDS), "Todos los hilos deberian terminar");
        assertFalse(violacion.get(),
            "No debe haber escritor activo con lectores, ni multiples escritores");
    }
}
