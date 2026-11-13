package com.example.springboot.service;

import com.example.springboot.dto.TradeRequest;
import com.example.springboot.dto.TradeResponse;
import com.example.springboot.entity.Trade;
import com.example.springboot.event.TradeCreatedEvent;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.TradeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Returns DTOs, not entities. Mapping happens here, inside the transaction, because
 * spring.jpa.open-in-view=false: a lazy field touched later in the controller or by
 * Jackson would throw LazyInitializationException.
 */
@Slf4j
@Service
@RequiredArgsConstructor // constructor injection (preferred over field @Autowired)
public class TradeService {

    private final TradeRepository tradeRepository;
    private final ApplicationEventPublisher events; // delivered after commit, see event/TradeCreatedListener

    @Transactional
    public TradeResponse create(TradeRequest request) {
        Trade saved = tradeRepository.save(request.toEntity());
        log.info("Created trade id={} symbol={}", saved.getId(), saved.getSymbol());
        events.publishEvent(TradeCreatedEvent.from(saved));
        return TradeResponse.from(saved);
    }

    /**
     * Returns all trades ordered by id, optionally filtered by type and/or userId.
     * Query-By-Example skips null probe fields, which handles every combination
     * of optional filters without writing four separate repository methods.
     */
    @Transactional(readOnly = true)
    public List<TradeResponse> findAll(String type, Integer userId) {
        log.debug("Listing trades type={} userId={}", type, userId);
        Trade probe = Trade.builder().type(type).userId(userId).build();
        return tradeRepository.findAll(Example.of(probe), Sort.by("id")).stream()
                .map(TradeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TradeResponse findById(Long id) {
        return TradeResponse.from(getTrade(id));
    }

    /** 404 if missing: Spring Data 3+ deleteById() silently ignores unknown ids. */
    @Transactional
    public void delete(Long id) {
        tradeRepository.delete(getTrade(id));
        log.info("Deleted trade id={}", id);
    }

    private Trade getTrade(Long id) {
        return tradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trade", id));
    }
}
