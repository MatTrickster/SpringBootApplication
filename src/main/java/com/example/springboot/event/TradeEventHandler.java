package com.example.springboot.event;

/**
 * What to do with a committed trade. Which implementation exists is decided by
 * "app.trade-events.enabled" (see config/TradeEventsConfig). WHEN it runs is decided
 * by TradeCreatedListener (after commit).
 */
public interface TradeEventHandler {

    void handle(TradeCreatedEvent event);

    /** Short name, exposed by GET /config so you can see which bean won. */
    String name();
}
