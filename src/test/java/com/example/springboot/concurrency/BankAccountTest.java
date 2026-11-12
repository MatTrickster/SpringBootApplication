package com.example.springboot.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankAccountTest {

    private ExecutorService pool;

    @BeforeEach
    void setUp() {
        pool = Executors.newFixedThreadPool(16);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void concurrentDeposits_noLostUpdates() throws Exception {
        BankAccount account = new BankAccount(0);
        int deposits = 10_000;
        // A start gate makes all threads hit the account at the same moment,
        // which is what exposes races (without synchronized this test fails).
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(deposits);

        for (int i = 0; i < deposits; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    account.deposit(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(account.balance()).isEqualTo(deposits);
    }

    @Test
    void concurrentWithdrawals_neverOverdraw() throws Exception {
        BankAccount account = new BankAccount(100);
        int attempts = 1_000;
        AtomicInteger successes = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(attempts);

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (account.withdraw(1)) {
                        successes.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(successes.get()).isEqualTo(100);
        assertThat(account.balance()).isZero();
    }
}
