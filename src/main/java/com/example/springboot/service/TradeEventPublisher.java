package com.example.springboot.service;

import com.example.springboot.entity.Trade;

/**
 * Notified after a trade is created. Which implementation exists is decided by
 * the "app.trade-events.enabled" property (see config/TradeEventsConfig).
 */
public interface TradeEventPublisher {

    void tradeCreated(Trade trade);

    /** Short name, exposed by GET /config so you can see which bean won. */
    String name();
}
