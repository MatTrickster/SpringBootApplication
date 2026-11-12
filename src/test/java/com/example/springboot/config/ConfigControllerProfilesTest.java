package com.example.springboot.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies profile resolution: default profile, profile overrides, fallback to
 * base application.properties, and @Profile-conditional beans.
 */
class ConfigControllerProfilesTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    class NoProfileActivated {
        @Autowired MockMvc mockMvc;

        @Test
        void fallsBackToDefaultDevProfile() throws Exception {
            mockMvc.perform(get("/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeProfiles[0]").value("dev"))
                    .andExpect(jsonPath("$.environmentName").value("development"))
                    .andExpect(jsonPath("$.featureXEnabled").value(true))
                    // not set in application-dev -> inherited from application.properties
                    .andExpect(jsonPath("$.maxPageSize").value(20))
                    .andExpect(jsonPath("$.notificationChannel").value("console"));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("qa")
    class QaProfile {
        @Autowired MockMvc mockMvc;

        @Test
        void usesQaOverrides() throws Exception {
            mockMvc.perform(get("/config"))
                    .andExpect(jsonPath("$.activeProfiles[0]").value("qa"))
                    .andExpect(jsonPath("$.environmentName").value("qa"))
                    .andExpect(jsonPath("$.maxPageSize").value(50))
                    .andExpect(jsonPath("$.supportEmail").value("support@example.com"));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("staging")
    class StagingProfile {
        @Autowired MockMvc mockMvc;

        @Test
        void usesStagingOverridesAndStagingOnlyBean() throws Exception {
            mockMvc.perform(get("/config"))
                    .andExpect(jsonPath("$.environmentName").value("staging"))
                    .andExpect(jsonPath("$.featureXEnabled").value(false))
                    .andExpect(jsonPath("$.maxPageSize").value(100))
                    .andExpect(jsonPath("$.supportEmail").value("staging-support@example.com"))
                    .andExpect(jsonPath("$.notificationChannel").value("email"));
        }
    }
}
