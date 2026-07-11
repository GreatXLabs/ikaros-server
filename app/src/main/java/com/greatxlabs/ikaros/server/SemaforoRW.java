package com.greatxlabs.ikaros.server;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SemaforoRW {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    public void iniciarLectura() throws InterruptedException {
        rwLock.readLock().lockInterruptibly();
    }

    public void terminarLectura() throws InterruptedException {
        rwLock.readLock().unlock();
    }

    public void iniciarEscritura() throws InterruptedException {
        rwLock.writeLock().lockInterruptibly();
    }

    public void terminarEscritura() {
        rwLock.writeLock().unlock();
    }
}
