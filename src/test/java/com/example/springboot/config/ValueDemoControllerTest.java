package com.example.springboot.config;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("qa")
class ValueDemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void valueInjection_resolvesProfileValuesDefaultsListsAndSpel() throws Exception {
        mockMvc.perform(get("/config/value-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationName").value("SpringBoot"))
                .andExpect(jsonPath("$.maxPageSize").value(50))          // qa override
                .andExpect(jsonPath("$.featureXEnabled").value(true))
                .andExpect(jsonPath("$.missingWithDefault").value("fallback-value"))
                .andExpect(jsonPath("$.allowedSymbols", contains("ABX", "AAPL", "MSFT")))
                .andExpect(jsonPath("$.maxPageSizeDoubled").value(100)); // SpEL
    }
}
