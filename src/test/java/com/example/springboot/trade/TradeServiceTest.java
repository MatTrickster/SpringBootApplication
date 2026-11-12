package com.example.springboot.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.springboot.exception.ResourceNotFoundException;
import java.util.Optional;

import com.example.springboot.dto.TradeRequest;
import com.example.springboot.entity.Trade;
import com.example.springboot.repository.TradeRepository;
import com.example.springboot.service.TradeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/** Plain unit test: no Spring context at all, so it runs in milliseconds. */
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository repository;

    @InjectMocks
    private TradeService service;

    @Test
    void create_mapsRequestToEntityAndSaves() {
        given(repository.save(any(Trade.class))).willAnswer(inv -> {
            Trade t = inv.getArgument(0);
            // Assert here: ArgumentCaptor keeps a reference, so it would see id=1 after we set it.
            assertThat(t.getId()).as("id must be generated, not supplied").isNull();
            t.setId(1L);
            return t;
        });

        Trade result = service.create(new TradeRequest("buy", 23, "ABX", 30, 134, 1531522701000L));

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(23);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSymbol()).isEqualTo("ABX");
    }

    @Test
    void findById_throwsWhenMissing() {
        given(repository.findById(42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }
}
