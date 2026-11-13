package com.example.springboot.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * app.portfolio.* settings. @Validated makes the app FAIL AT STARTUP on bad config
 * (e.g. threads=0) instead of failing on the first request.
 * Duration binds from "5s", "500ms", "PT5S".
 */
@Validated
@ConfigurationProperties(prefix = "app.portfolio")
public record PortfolioProperties(
        @Min(1) int threads,
        @Min(1) int queueCapacity,
        @NotNull Duration timeout) {
}
