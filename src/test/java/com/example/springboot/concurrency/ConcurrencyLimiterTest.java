package com.example.springboot.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConcurrencyLimiterTest {

    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    void neverRunsMoreThanMaxConcurrentTasks() throws Exception {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(3);
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxObserved = new AtomicInteger();

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(pool.submit(() -> limiter.execute(() -> {
                int now = running.incrementAndGet();
                maxObserved.accumulateAndGet(now, Math::max);
                Thread.sleep(20);
                running.decrementAndGet();
                return now;
            })));
        }
        for (Future<Integer> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }

        assertThat(maxObserved.get()).isBetween(1, 3);
        assertThat(limiter.availablePermits()).isEqualTo(3); // every permit returned
    }

    @Test
    void tryExecute_returnsEmptyWhenNoPermitBecomesFree() throws Exception {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(1);
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        // occupy the only permit
        pool.submit(() -> limiter.execute(() -> {
            holding.countDown();
            release.await();
            return "busy";
        }));
        assertThat(holding.await(2, TimeUnit.SECONDS)).isTrue();

        Optional<String> result = limiter.tryExecute(() -> "never", 50, TimeUnit.MILLISECONDS);
        assertThat(result).isEmpty();

        release.countDown();
    }

    @Test
    void permitIsReleasedEvenWhenTaskThrows() {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(2);

        assertThatThrownBy(() -> limiter.execute(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(limiter.availablePermits()).isEqualTo(2);
    }
}
