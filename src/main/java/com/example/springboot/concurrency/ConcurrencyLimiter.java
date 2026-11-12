package com.example.springboot.concurrency;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Semaphore: allow at most N tasks to run at the same time, e.g. to protect a slow
 * downstream service that can only handle N concurrent calls.
 *
 *   synchronized / Lock  -> exactly 1 thread at a time
 *   Semaphore(N)         -> up to N threads at a time
 *   fixed thread pool    -> also bounds concurrency, but owns the threads; a semaphore
 *                           limits callers that already run on their own threads (e.g. web requests)
 *
 * release() MUST be in finally: if the task throws and the permit isn't returned,
 * capacity leaks until nothing can run.
 */
public class ConcurrencyLimiter {

    private final Semaphore semaphore;

    public ConcurrencyLimiter(int maxConcurrent) {
        if (maxConcurrent < 1) {
            throw new IllegalArgumentException("maxConcurrent must be >= 1");
        }
        // fair = true: waiting threads get permits in FIFO order (no starvation)
        this.semaphore = new Semaphore(maxConcurrent, true);
    }

    /** Blocks until a permit is free, then runs the task. */
    public <T> T execute(Callable<T> task) throws Exception {
        semaphore.acquire();
        try {
            return task.call();
        } finally {
            semaphore.release();
        }
    }

    /**
     * Waits at most {@code timeout} for a permit. Returns empty if none became free:
     * the caller can fail fast (e.g. HTTP 429/503) instead of piling up threads.
     * The task must not return null.
     */
    public <T> Optional<T> tryExecute(Callable<T> task, long timeout, TimeUnit unit) throws Exception {
        if (!semaphore.tryAcquire(timeout, unit)) {
            return Optional.empty();
        }
        try {
            return Optional.of(task.call());
        } finally {
            semaphore.release();
        }
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
