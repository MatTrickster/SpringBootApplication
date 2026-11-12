package com.example.springboot.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springboot.entity.Trade;
import com.example.springboot.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * JPA slice: only entities, repositories and an embedded DB are loaded.
 * Each test runs in a transaction that is rolled back afterwards.
 */
@DataJpaTest
class TradeRepositoryTest {

    @Autowired
    private TradeRepository repository;

    @BeforeEach
    void seed() {
        repository.save(trade("buy", 1, "AAA", 10));
        repository.save(trade("buy", 1, "BBB", 5));
        repository.save(trade("sell", 1, "AAA", 3));
        repository.save(trade("buy", 2, "AAA", 100));
    }

    @Test
    void derivedQuery_findsBySymbol() {
        assertThat(repository.findBySymbolOrderByIdAsc("AAA")).hasSize(3);
    }

    @Test
    void jpqlQuery_sumsSharesForUserAndType() {
        assertThat(repository.totalSharesByUserAndType(1, "buy")).isEqualTo(15);
        assertThat(repository.totalSharesByUserAndType(99, "buy")).isZero();
    }

    private static Trade trade(String type, int userId, String symbol, int shares) {
        return Trade.builder().type(type).userId(userId).symbol(symbol)
                .shares(shares).price(10).timestamp(1L).build();
    }
}
