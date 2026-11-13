package com.example.springboot.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.springboot.dto.TradeRequest;
import com.example.springboot.dto.TradeResponse;
import com.example.springboot.event.TradeCreatedEvent;
import com.example.springboot.event.TradeEventHandler;
import com.example.springboot.repository.TradeRepository;
import com.example.springboot.service.TradeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * After-commit event delivery and transaction rollback, against the real DB.
 *
 * No @Transactional on the test class: a test transaction would never commit, so an
 * AFTER_COMMIT listener would never fire. TransactionTemplate gives explicit control
 * to build the "caller's transaction rolls back" scenario.
 */
@SpringBootTest
class TradeEventsIntegrationTest {

    @MockitoBean // replaces whichever conditional TradeEventHandler bean is active
    private TradeEventHandler handler;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static TradeRequest request() {
        return new TradeRequest("buy", 7, "EVT", 3, 10, 1L);
    }

    @Test
    void committedTrade_isDeliveredToHandler() {
        TradeResponse created = tradeService.create(request());

        ArgumentCaptor<TradeCreatedEvent> event = ArgumentCaptor.forClass(TradeCreatedEvent.class);
        verify(handler).handle(event.capture());
        assertThat(event.getValue().tradeId()).isEqualTo(created.id());
        assertThat(event.getValue().symbol()).isEqualTo("EVT");
    }

    @Test
    void handlerRunsOnlyAfterCommit_notInsideTheTransaction() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            tradeService.create(request()); // joins this outer transaction (REQUIRED)
            verify(handler, never()).handle(any()); // still uncommitted: nothing delivered yet
        });

        verify(handler).handle(any()); // delivered once the outer transaction committed
    }

    @Test
    void rolledBackTrade_isNeitherSavedNorDelivered() {
        long before = tradeRepository.count();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            tradeService.create(request());
            status.setRollbackOnly(); // e.g. a later step in the same business transaction failed
        });

        assertThat(tradeRepository.count()).as("insert rolled back").isEqualTo(before);
        verify(handler, never()).handle(any());
    }

    @Test
    void rollbackOnRuntimeException_undoesTheInsert() {
        long before = tradeRepository.count();

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                tradeService.create(request());
                throw new IllegalStateException("downstream step failed");
            });
        } catch (IllegalStateException expected) {
            // TransactionTemplate rolls back on RuntimeException and rethrows it
        }

        assertThat(tradeRepository.count()).isEqualTo(before);
        verify(handler, never()).handle(any());
    }

    @Test
    void failingHandler_doesNotUndoCommittedTrade() {
        doThrow(new IllegalStateException("audit service down")).when(handler).handle(any());

        TradeResponse created = tradeService.create(request());

        assertThat(tradeRepository.findById(created.id())).isPresent();
    }
}
