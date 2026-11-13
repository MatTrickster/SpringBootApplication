package com.example.springboot.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers TradeCreatedEvent only AFTER the publishing transaction commits.
 *
 * Why: if TradeService called the handler directly, a later failure in the same transaction
 * would roll the trade back while downstream consumers had already been told it exists.
 *
 * Gotchas worth knowing in an interview:
 *  - Published outside a transaction, a @TransactionalEventListener is silently skipped
 *    (unless fallbackExecution = true). TradeService.create is @Transactional, so it fires.
 *  - It runs synchronously on the caller's thread, after commit. Slow work -> add @Async.
 *  - The transaction is already committed: DB writes here need @Transactional(REQUIRES_NEW).
 *  - Plain @EventListener runs immediately, inside the transaction, rollback or not.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeCreatedListener {

    private final TradeEventHandler handler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTradeCreated(TradeCreatedEvent event) {
        try {
            handler.handle(event);
        } catch (RuntimeException e) {
            // The trade is committed. A failing side effect must not turn that into an error
            // response for the client, so log it (a real system would retry or use an outbox).
            log.error("Trade event handler failed for trade {}", event.tradeId(), e);
        }
    }
}
