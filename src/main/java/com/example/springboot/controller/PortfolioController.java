package com.example.springboot.controller;

import com.example.springboot.dto.PortfolioSummary;
import com.example.springboot.service.PortfolioService;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /portfolio?userIds=1,2,3 -> one summary per user, computed in parallel.
 *   400 missing/non-numeric/too many ids, 503 workers busy, 504 timed out
 */
@RestController
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    // @Size bounds the fan-out so one request can't fill the shared pool's queue on its own.
    @GetMapping("/portfolio")
    public List<PortfolioSummary> portfolio(@RequestParam @Size(min = 1, max = 50) List<Integer> userIds) {
        return portfolioService.summarize(userIds);
    }
}
