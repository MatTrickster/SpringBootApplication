package com.example.springboot.controller;

import com.example.springboot.entity.Trade;
import com.example.springboot.dto.TradeRequest;
import com.example.springboot.service.TradeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Classic HackerRank "Trades API":
 *   POST   /trades          -> 201 + created trade (ids 1, 2, 3, ...)
 *   GET    /trades          -> 200 + all trades by id asc, optional ?type=&userId=
 *   GET    /trades/{id}     -> 200, or 404 if missing
 *   DELETE /trades/{id}     -> 204, or 404 if missing
 *   PUT/PATCH /trades/{id}  -> 405 (no handler mapped, Spring returns it for us)
 */
@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Trade create(@Valid @RequestBody TradeRequest request) {
        return tradeService.create(request);
    }

    @GetMapping
    public List<Trade> findAll(@RequestParam(required = false) String type,
                               @RequestParam(required = false) Integer userId) {
        return tradeService.findAll(type, userId);
    }

    @GetMapping("/{id}")
    public Trade findById(@PathVariable Long id) {
        return tradeService.findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        tradeService.delete(id);
    }
}
