package com.example.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding of every "app.*" property. Values come from
 * application.properties and are overridden by application-{profile}.properties.
 *
 * app.environment-name  -> environmentName   (relaxed binding: kebab-case -> camelCase)
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String environmentName,
        boolean featureXEnabled,
        int maxPageSize,
        String supportEmail) {
}
