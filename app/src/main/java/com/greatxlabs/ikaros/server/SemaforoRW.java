package com.greatxlabs.ikaros.server;

import java.util.concurrent.Semaphore;

public class SemaforoRW {

    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore recurso = new Semaphore(1, true);
    private final Semaphore turno = new Semaphore(1, true);
    private int lectoresActivos = 0;

    public void iniciarLectura() throws InterruptedException {
        turno.acquire();
        Thread.sleep(2500);
        mutex.acquire();
        Thread.sleep(2500);
        lectoresActivos++;
        if (lectoresActivos == 1) {
            try {
                recurso.acquire();
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                lectoresActivos--;
                mutex.release();
                turno.release();
                throw e;
            }
        }
        mutex.release();
        turno.release();
    }

    public void terminarLectura() throws InterruptedException {
        mutex.acquire();
        Thread.sleep(2500);
        lectoresActivos--;
        if (lectoresActivos == 0) {
            recurso.release();
        }
        mutex.release();
    }

    public void iniciarEscritura() throws InterruptedException {
        turno.acquire();
        Thread.sleep(2500);
        try {
            recurso.acquire();
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            turno.release();
            throw e;
        }
    }

    public void terminarEscritura() {
        recurso.release();
        turno.release();
    }
}
