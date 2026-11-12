package com.example.springboot.config;

import com.example.springboot.entity.Trade;
import com.example.springboot.repository.TradeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner: runs once, after the application context is fully started.
 *
 * data.sql vs CommandLineRunner:
 *  - data.sql: plain SQL, runs in every profile, no Java logic.
 *  - CommandLineRunner: Java code with access to repositories and services, can be
 *    restricted with @Profile/@Conditional, can check existing data first.
 *
 * Here: sample trades are seeded in dev only, so qa/staging start empty.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TradeRepository tradeRepository;

    @Override
    public void run(String... args) {
        if (tradeRepository.count() > 0) {
            log.debug("Trades already present, skipping seed");
            return;
        }
        List<Trade> samples = List.of(
                trade("buy", 1, "ABX", 30, 134),
                trade("sell", 1, "ABX", 10, 140),
                trade("buy", 2, "AAPL", 5, 190),
                trade("buy", 3, "MSFT", 12, 410));
        tradeRepository.saveAll(samples);
        log.info("Seeded {} sample trades (dev profile only)", samples.size());
    }

    private static Trade trade(String type, int userId, String symbol, int shares, int price) {
        return Trade.builder().type(type).userId(userId).symbol(symbol)
                .shares(shares).price(price).timestamp(System.currentTimeMillis()).build();
    }
}
