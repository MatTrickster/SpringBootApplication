package com.example.springboot.service;

import com.example.springboot.dto.PortfolioSummary;
import com.example.springboot.repository.TradeRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
 *
 * Alternative: CompletableFuture.supplyAsync(() -> ..., executor) + join(), which
 * composes better (thenApply, allOf) but needs care to apply a timeout (orTimeout).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final long TIMEOUT_SECONDS = 5;

    private final TradeRepository tradeRepository;
    private final ExecutorService portfolioExecutor;

    public List<PortfolioSummary> summarize(List<Integer> userIds) {
        List<Callable<PortfolioSummary>> tasks = userIds.stream()
                .distinct()
                .map(id -> (Callable<PortfolioSummary>) () -> summarizeOne(id))
                .toList();
        try {
            List<Future<PortfolioSummary>> futures =
                    portfolioExecutor.invokeAll(tasks, TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<PortfolioSummary> results = new ArrayList<>(futures.size());
            for (Future<PortfolioSummary> future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // never swallow an interrupt: restore the flag
            throw new IllegalStateException("Interrupted while computing portfolios", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Portfolio calculation failed", e.getCause());
        } catch (CancellationException e) {
            throw new IllegalStateException("Portfolio calculation timed out after " + TIMEOUT_SECONDS + "s", e);
        }
    }

    private PortfolioSummary summarizeOne(Integer userId) {
        log.debug("Computing portfolio for user {} on {}", userId, Thread.currentThread().getName());
        long bought = tradeRepository.totalSharesByUserAndType(userId, "buy");
        long sold = tradeRepository.totalSharesByUserAndType(userId, "sell");
        return new PortfolioSummary(userId, bought, sold, bought - sold);
    }
}
