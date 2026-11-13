package com.example.springboot.service;

import com.example.springboot.config.PortfolioProperties;
import com.example.springboot.dto.PortfolioSummary;
import com.example.springboot.exception.OperationTimeoutException;
import com.example.springboot.exception.ServiceOverloadedException;
import com.example.springboot.repository.TradeRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ExecutorService in a Spring service: compute each user's portfolio in parallel.
 *
 * invokeAll(tasks, timeout):
 *  - submits every task, waits until all finish OR the timeout expires
 *  - returns futures in the SAME ORDER as the tasks (so results match input order)
 *  - tasks still running at the timeout are cancelled -> future.get() throws CancellationException
 *  - if the pool rejects a task, it cancels the ones already submitted and rethrows
 *
 * Failure -> HTTP mapping (GlobalExceptionHandler):
 *   pool and queue full        RejectedExecutionException -> ServiceOverloadedException -> 503
 *   time budget exceeded       CancellationException      -> OperationTimeoutException  -> 504
 *   a task threw               ExecutionException         -> IllegalStateException      -> 500 (a real bug)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final TradeRepository tradeRepository;
    private final ExecutorService portfolioExecutor;
    private final PortfolioProperties properties;

    public List<PortfolioSummary> summarize(List<Integer> userIds) {
        List<Callable<PortfolioSummary>> tasks = userIds.stream()
                .distinct()
                .map(id -> (Callable<PortfolioSummary>) () -> summarizeOne(id))
                .toList();

        List<Future<PortfolioSummary>> futures;
        try {
            futures = portfolioExecutor.invokeAll(tasks, properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            throw new ServiceOverloadedException("Portfolio workers are busy, retry shortly", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // never swallow an interrupt: restore the flag
            throw new IllegalStateException("Interrupted while computing portfolios", e);
        }

        // invokeAll has returned, so every future is done (completed, failed or cancelled): get() won't block.
        List<PortfolioSummary> results = new ArrayList<>(futures.size());
        for (Future<PortfolioSummary> future : futures) {
            results.add(resultOf(future));
        }
        return results;
    }

    private PortfolioSummary resultOf(Future<PortfolioSummary> future) {
        try {
            return future.get();
        } catch (CancellationException e) {
            throw new OperationTimeoutException("Portfolio calculation", properties.timeout(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Portfolio calculation failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading portfolio result", e);
        }
    }

    private PortfolioSummary summarizeOne(Integer userId) {
        log.debug("Computing portfolio for user {} on {}", userId, Thread.currentThread().getName());
        long bought = tradeRepository.totalSharesByUserAndType(userId, "buy");
        long sold = tradeRepository.totalSharesByUserAndType(userId, "sell");
        return new PortfolioSummary(userId, bought, sold, bought - sold);
    }
}
