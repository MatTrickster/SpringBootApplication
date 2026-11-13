package com.example.springboot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for a trade record.
 *
 * Wrapper types (Long/Integer) are used instead of primitives on purpose:
 * Query-By-Example ignores null fields, so a probe Trade with only "type"
 * set filters on type alone. Primitives would default to 0 and break that.
 *
 * Indexes match the real query patterns:
 *  - (user_id, type): GET /trades?userId=&type= and the portfolio SUM query. A composite
 *    index also serves queries on user_id alone (leftmost prefix), not on type alone.
 *  - symbol: findBySymbolOrderByIdAsc.
 * ddl-auto creates them here; in production they belong in a Flyway/Liquibase migration.
 */
@Entity
@Table(name = "trade", indexes = {
        @Index(name = "idx_trade_user_type", columnList = "user_id, type"),
        @Index(name = "idx_trade_symbol", columnList = "symbol")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    // IDENTITY -> H2 auto-increment, so the first trade gets id 1, then 2, ...
    // (a very common HackerRank requirement)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;      // "buy" | "sell"
    private Integer userId;
    private String symbol;
    private Integer shares;
    private Integer price;
    private Long timestamp;
}
