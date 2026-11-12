package com.example.springboot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 */
@Entity
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
