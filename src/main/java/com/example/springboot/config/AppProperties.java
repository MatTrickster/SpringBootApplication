package com.example.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding of the "app.*" properties. Values come from
 * application.properties and are overridden by application-{profile}.properties.
 *
 * app.environment-name  -> environmentName   (relaxed binding: kebab-case -> camelCase)
 *
 * @ConfigurationProperties vs @Value (see controller/ValueDemoController):
 *  - groups related settings into one immutable, testable object
 *  - relaxed binding (APP_DEFAULT_USER env var also maps to defaultUser)
 *  - can be validated with @Validated + constraints
 *  - @Value is fine for one-off values and supports SpEL; this does not.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String environmentName,
        boolean featureXEnabled,
        int maxPageSize,
        String supportEmail,
        String defaultUser) {
}
