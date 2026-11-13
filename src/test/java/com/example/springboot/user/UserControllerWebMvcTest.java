package com.example.springboot.user;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springboot.controller.UserController;
import com.example.springboot.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    /** The race the service-level check can't catch: the DB constraint fires instead. */
    @Test
    void dbUniqueViolation_returns409_withoutLeakingSql() throws Exception {
        given(userService.create(any())).willThrow(new DataIntegrityViolationException(
                "could not execute statement [Unique index or primary key violation: PUBLIC.CONSTRAINT_INDEX_4]"));

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"racer","email":"racer@example.com","role":"USER"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Request conflicts with existing data"))
                .andExpect(content().string(not(containsString("CONSTRAINT"))));
    }
}
