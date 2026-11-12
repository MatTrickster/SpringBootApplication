package com.example.springboot.trade;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Full-stack test: real controller + service + JPA + H2, HTTP simulated by MockMvc.
 * This is the shape of most HackerRank Spring Boot grading tests.
 *
 * @Sql resets the table AND the identity counter before each test, so
 * "first trade has id 1" holds regardless of test order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = "TRUNCATE TABLE trade RESTART IDENTITY")
class TradeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static String tradeJson(String type, int userId, String symbol) {
        return """
                {"type":"%s","userId":%d,"symbol":"%s","shares":10,"price":150,"timestamp":1531522701000}
                """.formatted(type, userId, symbol);
    }

    private ResultActions postTrade(String json) throws Exception {
        return mockMvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void createTrade_returns201AndAssignsSequentialIds() throws Exception {
        postTrade(tradeJson("buy", 23, "ABX"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.symbol").value("ABX"));

        postTrade(tradeJson("sell", 23, "ABX"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void getAll_returnsTradesOrderedById() throws Exception {
        postTrade(tradeJson("buy", 1, "AAA"));
        postTrade(tradeJson("sell", 2, "BBB"));

        mockMvc.perform(get("/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getAll_filtersByTypeAndUserId() throws Exception {
        postTrade(tradeJson("buy", 1, "AAA"));
        postTrade(tradeJson("sell", 1, "AAA"));
        postTrade(tradeJson("buy", 2, "BBB"));

        mockMvc.perform(get("/trades").param("type", "buy").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));

        mockMvc.perform(get("/trades").param("type", "buy"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        mockMvc.perform(get("/trades/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Trade not found with id 99"));
    }

    @Test
    void createTrade_returns400ForInvalidBody() throws Exception {
        postTrade("""
                {"type":"hold","userId":1,"symbol":"","shares":500,"price":10,"timestamp":1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists())
                .andExpect(jsonPath("$.errors.symbol").exists())
                .andExpect(jsonPath("$.errors.shares").exists());
    }

    @Test
    void updateAndDelete_return405() throws Exception {
        mockMvc.perform(put("/trades/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/trades/1"))
                .andExpect(status().isMethodNotAllowed());
    }
}
