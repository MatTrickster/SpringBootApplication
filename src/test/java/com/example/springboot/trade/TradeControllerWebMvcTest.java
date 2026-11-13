package com.example.springboot.trade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.controller.TradeController;
import com.example.springboot.dto.TradeResponse;
import com.example.springboot.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer slice: only MVC infrastructure + this controller + @ControllerAdvice
 * are loaded. No JPA, no DB. The service is replaced with a Mockito mock.
 *
 * Boot 3.4+ / 4.x: @MockitoBean   (Boot 2.x / 3.0-3.3: @MockBean)
 */
@WebMvcTest(TradeController.class)
class TradeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeService tradeService;

    @Test
    void getById_delegatesToService() throws Exception {
        given(tradeService.findById(5L)).willReturn(
                new TradeResponse(5L, "buy", 1, "XYZ", 1, 1, 1L));

        mockMvc.perform(get("/trades/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("XYZ"));
    }

    @Test
    void getById_mapsNotFoundExceptionTo404() throws Exception {
        given(tradeService.findById(7L)).willThrow(new ResourceNotFoundException("Trade", 7L));

        mockMvc.perform(get("/trades/7")).andExpect(status().isNotFound());
    }

    @Test
    void create_invalidBody_neverReachesService() throws Exception {
        mockMvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(tradeService, never()).create(any());
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }
}
