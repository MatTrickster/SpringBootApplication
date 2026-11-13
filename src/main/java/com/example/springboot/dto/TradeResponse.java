package com.example.springboot.dto;

import com.example.springboot.entity.Trade;

/**
 * Public JSON contract for a trade. Decoupled from the JPA entity so that renaming a
 * column, adding an internal field or a lazy relation never silently changes the API.
 * (Today it has the same fields as the entity, which is the HackerRank Trades contract.)
 */
public record TradeResponse(Long id,
                            String type,
                            Integer userId,
                            String symbol,
                            Integer shares,
                            Integer price,
                            Long timestamp) {

    public static TradeResponse from(Trade trade) {
        return new TradeResponse(trade.getId(), trade.getType(), trade.getUserId(), trade.getSymbol(),
                trade.getShares(), trade.getPrice(), trade.getTimestamp());
    }
}
