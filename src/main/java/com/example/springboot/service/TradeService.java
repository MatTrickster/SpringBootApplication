package com.example.springboot.service;

import com.example.springboot.exception.ResourceNotFoundException;
import java.util.List;

import com.example.springboot.repository.TradeRepository;
import com.example.springboot.entity.Trade;
import com.example.springboot.dto.TradeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor // constructor injection (preferred over field @Autowired)
public class TradeService {

    private final TradeRepository tradeRepository;
    private final TradeEventPublisher tradeEventPublisher; // which bean: see config/TradeEventsConfig

    @Transactional
    public Trade create(TradeRequest request) {
        Trade saved = tradeRepository.save(request.toEntity());
        log.info("Created trade id={} symbol={}", saved.getId(), saved.getSymbol());
        tradeEventPublisher.tradeCreated(saved);
        return saved;
    }

    /**
     * Returns all trades ordered by id, optionally filtered by type and/or userId.
     * Query-By-Example skips null probe fields, which handles every combination
     * of optional filters without writing four separate repository methods.
     */
    @Transactional(readOnly = true)
    public List<Trade> findAll(String type, Integer userId) {
        log.debug("Listing trades type={} userId={}", type, userId);
        Trade probe = Trade.builder().type(type).userId(userId).build();
        return tradeRepository.findAll(Example.of(probe), Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Trade findById(Long id) {
        return tradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trade", id));
    }

    /** 404 if missing: Spring Data 3+ deleteById() silently ignores unknown ids. */
    @Transactional
    public void delete(Long id) {
        Trade trade = findById(id);
        tradeRepository.delete(trade);
        log.info("Deleted trade id={}", id);
    }
}
