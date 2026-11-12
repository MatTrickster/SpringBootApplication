package com.example.springboot.portfolio;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * No @Transactional here on purpose: the work runs on executor threads, which don't
 * see an uncommitted test transaction. Data is inserted and committed by @Sql instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
        "TRUNCATE TABLE trade RESTART IDENTITY",
        "INSERT INTO trade (type, user_id, symbol, shares, price, timestamp) VALUES "
                + "('buy', 1, 'ABX', 30, 10, 1), ('sell', 1, 'ABX', 10, 10, 2), "
                + "('buy', 2, 'AAPL', 5, 10, 3)"
})
class PortfolioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void summarizesEachUserInRequestOrder() throws Exception {
        mockMvc.perform(get("/portfolio").param("userIds", "2,1,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].netShares").value(5))
                .andExpect(jsonPath("$[1].userId").value(1))
                .andExpect(jsonPath("$[1].sharesBought").value(30))
                .andExpect(jsonPath("$[1].sharesSold").value(10))
                .andExpect(jsonPath("$[1].netShares").value(20))
                .andExpect(jsonPath("$[2].netShares").value(0)); // user 3 has no trades
    }

    @Test
    void duplicateIdsAreComputedOnce() throws Exception {
        mockMvc.perform(get("/portfolio").param("userIds", "1,1,1"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void missingUserIds_returns400() throws Exception {
        mockMvc.perform(get("/portfolio")).andExpect(status().isBadRequest());
    }
}
