package com.example.springboot.event;

import com.example.springboot.entity.Trade;

/**
 * Immutable snapshot of a created trade. Events carry values, not the JPA entity:
 * the entity is detached once the transaction ends and could be mutated by the publisher.
 */
public record TradeCreatedEvent(Long tradeId, Integer userId, String type, String symbol, Integer shares) {

    public static TradeCreatedEvent from(Trade trade) {
        return new TradeCreatedEvent(trade.getId(), trade.getUserId(), trade.getType(),
                trade.getSymbol(), trade.getShares());
    }
}
