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
 * base application.properties, @Profile beans and @ConditionalOnProperty beans.
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
                    .andExpect(jsonPath("$.defaultUser").value("admin"))
                    .andExpect(jsonPath("$.notificationChannel").value("console"))
                    .andExpect(jsonPath("$.tradeEventPublisher").value("logging"));
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
                    .andExpect(jsonPath("$.supportEmail").value("support@example.com"))
                    .andExpect(jsonPath("$.defaultUser").value("qa-user"))
                    // app.trade-events.enabled=false -> havingValue="false" branch
                    .andExpect(jsonPath("$.tradeEventPublisher").value("noop"));
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
                    // staging doesn't set app.default-user -> inherited from application.properties
                    .andExpect(jsonPath("$.defaultUser").value("alice"))
                    .andExpect(jsonPath("$.notificationChannel").value("email"))
                    // property missing entirely -> matchIfMissing = true picks no-op
                    .andExpect(jsonPath("$.tradeEventPublisher").value("noop"));
        }
    }

    /** Flip a @ConditionalOnProperty from a test, without creating a new profile. */
    @Nested
    @SpringBootTest(properties = "app.trade-events.enabled=true")
    @AutoConfigureMockMvc
    @ActiveProfiles("staging")
    class StagingWithEventsForcedOn {
        @Autowired MockMvc mockMvc;

        @Test
        void propertyOverrideSelectsLoggingPublisher() throws Exception {
            mockMvc.perform(get("/config"))
                    .andExpect(jsonPath("$.environmentName").value("staging"))
                    .andExpect(jsonPath("$.tradeEventPublisher").value("logging"));
        }
    }
}
