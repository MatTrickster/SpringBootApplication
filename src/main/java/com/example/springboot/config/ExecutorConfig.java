package com.example.springboot.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A Spring-managed thread pool. Declaring it as a bean (instead of calling
 * Executors.newFixedThreadPool inside a service) means:
 *  - one shared pool, not a new pool per request (a classic leak)
 *  - Spring calls shutdown() when the context closes
 *  - tests can inject or replace it
 */
@Configuration
public class ExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    ExecutorService portfolioExecutor() {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory named = runnable -> {
            Thread t = new Thread(runnable, "portfolio-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        // Fixed size bounds concurrency. Java 21+: Executors.newVirtualThreadPerTaskExecutor()
        return Executors.newFixedThreadPool(4, named);
    }
}
