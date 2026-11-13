package com.example.springboot.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A bounded, Spring-managed thread pool.
 *
 * Why not Executors.newFixedThreadPool(4)? Its threads are bounded but its queue is an
 * unbounded LinkedBlockingQueue: under a traffic spike tasks pile up in memory until
 * latency explodes or the JVM runs out of heap. Here both are bounded:
 *
 *   threads           fixed (core = max), from app.portfolio.threads
 *   queue             ArrayBlockingQueue(app.portfolio.queue-capacity)
 *   when both full    AbortPolicy -> RejectedExecutionException -> PortfolioService -> 503
 *
 * Rejection policy alternatives:
 *   CallerRunsPolicy  natural back-pressure, but runs the work on the Tomcat request thread
 *   Discard(Oldest)   silently drops work: almost never right for request/response code
 */
@Configuration
public class ExecutorConfig {

    @Bean(destroyMethod = "shutdown") // Spring shuts the pool down when the context closes
    ThreadPoolExecutor portfolioExecutor(PortfolioProperties props) {
        return new ThreadPoolExecutor(
                props.threads(),
                props.threads(),
                0L, TimeUnit.MILLISECONDS,          // keep-alive is irrelevant when core == max
                new ArrayBlockingQueue<>(props.queueCapacity()),
                namedThreadFactory("portfolio-"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** Named threads make thread dumps and logs readable ("portfolio-3" vs "pool-7-thread-3"). */
    static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
