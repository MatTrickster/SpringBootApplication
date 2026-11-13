package com.example.springboot.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.example.springboot.config.PortfolioProperties;
import com.example.springboot.dto.PortfolioSummary;
import com.example.springboot.exception.OperationTimeoutException;
import com.example.springboot.exception.ServiceOverloadedException;
import com.example.springboot.repository.TradeRepository;
import com.example.springboot.service.PortfolioService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Failure modes of the bounded pool, tested with a real tiny ThreadPoolExecutor and a
 * mocked repository that can be made slow or blocking on demand. No Spring context.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // some tasks are cancelled before calling the mock
class PortfolioServiceTest {

    @Mock
    private TradeRepository repository;

    private ThreadPoolExecutor executor;

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private PortfolioService service(int threads, int queueCapacity, Duration timeout) {
        executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), new ThreadPoolExecutor.AbortPolicy());
        return new PortfolioService(repository, executor, new PortfolioProperties(threads, queueCapacity, timeout));
    }

    @Test
    void computesSummariesInInputOrder() {
        given(repository.totalSharesByUserAndType(1, "buy")).willReturn(30L);
        given(repository.totalSharesByUserAndType(1, "sell")).willReturn(10L);
        given(repository.totalSharesByUserAndType(2, "buy")).willReturn(5L);
        given(repository.totalSharesByUserAndType(2, "sell")).willReturn(0L);

        List<PortfolioSummary> result = service(2, 10, Duration.ofSeconds(5)).summarize(List.of(2, 1));

        assertThat(result).containsExactly(new PortfolioSummary(2, 5, 0, 5), new PortfolioSummary(1, 30, 10, 20));
    }

    @Test
    void slowWork_isCancelledAndReportedAsTimeout() {
        given(repository.totalSharesByUserAndType(anyInt(), anyString())).willAnswer(inv -> {
            Thread.sleep(2_000); // interrupted when invokeAll cancels the task
            return 0L;
        });

        PortfolioService service = service(2, 10, Duration.ofMillis(100));
        long startNanos = System.nanoTime();

        assertThatThrownBy(() -> service.summarize(List.of(1, 2)))
                .isInstanceOf(OperationTimeoutException.class)
                .hasMessage("Portfolio calculation did not complete within 100 ms");
        assertThat(Duration.ofNanos(System.nanoTime() - startNanos))
                .as("must give up at the timeout, not wait for the slow work")
                .isLessThan(Duration.ofSeconds(1));
    }

    /**
     * 1 thread + queue of 1: task 1 runs (and blocks), task 2 is queued, task 3 is rejected.
     * This is deterministic: ThreadPoolExecutor hands task 1 straight to a new worker.
     */
    @Test
    void saturatedPool_isReportedAsOverloaded() {
        CountDownLatch release = new CountDownLatch(1);
        given(repository.totalSharesByUserAndType(anyInt(), anyString())).willAnswer(inv -> {
            release.await();
            return 0L;
        });

        PortfolioService service = service(1, 1, Duration.ofSeconds(5));
        try {
            assertThatThrownBy(() -> service.summarize(List.of(1, 2, 3)))
                    .isInstanceOf(ServiceOverloadedException.class)
                    .hasCauseInstanceOf(RejectedExecutionException.class);
        } finally {
            release.countDown();
        }
    }

    @Test
    void failingTask_isReportedWithItsCause() {
        given(repository.totalSharesByUserAndType(eq(1), anyString()))
                .willThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service(2, 10, Duration.ofSeconds(5)).summarize(List.of(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Portfolio calculation failed")
                .hasRootCauseMessage("db down");
    }
}
