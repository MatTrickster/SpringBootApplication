package com.example.springboot.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.springboot.dto.TradeRequest;
import com.example.springboot.dto.TradeResponse;
import com.example.springboot.entity.Trade;
import com.example.springboot.event.TradeCreatedEvent;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.TradeRepository;
import com.example.springboot.service.TradeService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Plain unit test: no Spring context at all, so it runs in milliseconds. */
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository repository;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private TradeService service;

    @Test
    void create_mapsRequestToEntitySavesAndPublishesEvent() {
        given(repository.save(any(Trade.class))).willAnswer(inv -> {
            Trade t = inv.getArgument(0);
            // Assert here: ArgumentCaptor keeps a reference, so it would see id=1 after we set it.
            assertThat(t.getId()).as("id must be generated, not supplied").isNull();
            t.setId(1L);
            return t;
        });

        TradeResponse result = service.create(new TradeRequest("buy", 23, "ABX", 30, 134, 1531522701000L));

        ArgumentCaptor<Trade> saved = ArgumentCaptor.forClass(Trade.class);
        Mockito.verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(23);
        assertThat(result).isEqualTo(new TradeResponse(1L, "buy", 23, "ABX", 30, 134, 1531522701000L));

        ArgumentCaptor<TradeCreatedEvent> event = ArgumentCaptor.forClass(TradeCreatedEvent.class);
        Mockito.verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new TradeCreatedEvent(1L, 23, "buy", "ABX", 30));
    }

    @Test
    void findById_throwsWhenMissing() {
        given(repository.findById(42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void delete_missingTrade_throwsAndDeletesNothing() {
        given(repository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        Mockito.verify(repository, Mockito.never()).delete(any(Trade.class));
    }

    @Test
    void delete_existingTrade_deletesIt() {
        Trade trade = Trade.builder().id(3L).build();
        given(repository.findById(3L)).willReturn(Optional.of(trade));

        service.delete(3L);

        Mockito.verify(repository).delete(trade);
    }
}
