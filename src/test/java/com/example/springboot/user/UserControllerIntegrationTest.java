package com.example.springboot.user;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Users come from data.sql: 1 admin(ADMIN), 2 qa-user(QA), 3 alice(USER), 4 bob(USER, inactive).
 *
 * @Transactional on a MockMvc test rolls back after each test, because MockMvc runs
 * the request on the test thread. (It would NOT work with a real HTTP server via
 * TestRestTemplate: the server handles the request on another thread and commits.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions postUser(String json) throws Exception {
        return mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ---------- lookups ----------

    @Test
    void getById_returnsSeededUser() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User not found with id 999"));
    }

    @Test
    void defaultUser_inDevProfile_isAdmin() throws Exception {
        mockMvc.perform(get("/users/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    // ---------- pagination + filtering ----------

    @Test
    void list_isPagedAndSortedById() throws Exception {
        mockMvc.perform(get("/users").param("page", "0").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].username").value("admin"))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/users").param("page", "1").param("size", "3"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username").value("bob"))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void list_filtersByRole_caseInsensitive() throws Exception {
        mockMvc.perform(get("/users").param("role", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].username").value("alice"));
    }

    @Test
    void list_unknownRole_returns400() throws Exception {
        mockMvc.perform(get("/users").param("role", "superuser"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_pageSizeIsCappedByMaxPageSize() throws Exception {
        mockMvc.perform(get("/users").param("size", "1000"))
                .andExpect(jsonPath("$.size").value(20)); // app.max-page-size in dev
    }

    // ---------- create + business validation ----------

    @Test
    void create_returns201() throws Exception {
        postUser("""
                {"username":"carol","email":"Carol@Example.com","role":"USER"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("carol@example.com")) // normalized
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_duplicateUsername_returns409() throws Exception {
        postUser("""
                {"username":"admin","email":"new@example.com","role":"USER"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("User with username 'admin' already exists"));
    }

    @Test
    void create_duplicateEmailDifferentCase_returns409() throws Exception {
        postUser("""
                {"username":"alice2","email":"ALICE@example.com","role":"USER"}
                """)
                .andExpect(status().isConflict());
    }

    @Test
    void create_invalidBody_returns400WithFieldErrors() throws Exception {
        postUser("""
                {"username":"No Spaces!","email":"not-an-email"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    @Test
    void create_unknownEnumValue_returns400() throws Exception {
        postUser("""
                {"username":"dave","email":"dave@example.com","role":"SUPERUSER"}
                """)
                .andExpect(status().isBadRequest());
    }

    // ---------- default user per profile ----------

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("qa")
    class QaProfile {
        @Autowired MockMvc mockMvc;

        @Test
        void defaultUser_isQaUser() throws Exception {
            mockMvc.perform(get("/users/default"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("qa-user"))
                    .andExpect(jsonPath("$.role").value("QA"));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("staging")
    class StagingProfile {
        @Autowired MockMvc mockMvc;

        @Test
        void defaultUser_fallsBackToBaseProperty() throws Exception {
            mockMvc.perform(get("/users/default"))
                    .andExpect(jsonPath("$.username").value("alice"));
        }
    }

    @Nested
    @SpringBootTest(properties = "app.default-user=ghost")
    @AutoConfigureMockMvc
    class MisconfiguredDefaultUser {
        @Autowired MockMvc mockMvc;

        @Test
        void unknownDefaultUser_returns404() throws Exception {
            mockMvc.perform(get("/users/default"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("User not found with username ghost"));
        }
    }
}
