package com.example.springboot.portfolio;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springboot.controller.PortfolioController;
import com.example.springboot.exception.OperationTimeoutException;
import com.example.springboot.exception.ServiceOverloadedException;
import com.example.springboot.service.PortfolioService;
import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP mapping of the portfolio failure modes. */
@WebMvcTest(PortfolioController.class)
class PortfolioControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioService portfolioService;

    @Test
    void overloaded_returns503WithRetryAfter() throws Exception {
        given(portfolioService.summarize(anyList())).willThrow(
                new ServiceOverloadedException("Portfolio workers are busy, retry shortly",
                        new RejectedExecutionException()));

        mockMvc.perform(get("/portfolio").param("userIds", "1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(jsonPath("$.detail").value("Portfolio workers are busy, retry shortly"));
    }

    @Test
    void timeout_returns504() throws Exception {
        given(portfolioService.summarize(anyList())).willThrow(
                new OperationTimeoutException("Portfolio calculation", Duration.ofSeconds(5), null));

        mockMvc.perform(get("/portfolio").param("userIds", "1"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.detail").value("Portfolio calculation did not complete within 5000 ms"));
    }

    @Test
    void tooManyUserIds_returns400_beforeReachingService() throws Exception {
        String ids = IntStream.rangeClosed(1, 51).mapToObj(String::valueOf).collect(Collectors.joining(","));

        mockMvc.perform(get("/portfolio").param("userIds", ids))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userIds").exists());
        verifyNoInteractions(portfolioService);
    }

    @Test
    void nonNumericUserId_returns400() throws Exception {
        mockMvc.perform(get("/portfolio").param("userIds", "1,abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(portfolioService);
    }
}
