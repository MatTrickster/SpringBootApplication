package com.example.springboot.controller;

import com.example.springboot.dto.PortfolioSummary;
import com.example.springboot.service.PortfolioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** GET /portfolio?userIds=1,2,3 -> one summary per user, computed in parallel. */
@RestController
@RequiredArgsConstructor
public class PortfolioController {

    private static final int MAX_USERS = 50;

    private final PortfolioService portfolioService;

    @GetMapping("/portfolio")
    public List<PortfolioSummary> portfolio(@RequestParam List<Integer> userIds) {
        // Bound the fan-out so one request can't queue unlimited work on the shared pool.
        if (userIds.isEmpty() || userIds.size() > MAX_USERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "userIds must contain between 1 and " + MAX_USERS + " ids");
        }
        return portfolioService.summarize(userIds);
    }
}
