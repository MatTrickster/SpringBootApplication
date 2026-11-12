package com.example.springboot.config;

import com.example.springboot.service.NotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Profile-specific beans. Exactly one NotificationService is created:
 *   staging      -> email
 *   anything else (dev, qa, default) -> console
 */
@Configuration
public class NotificationConfig {

    @Bean
    @Profile("staging")
    NotificationService emailNotificationService() {
        return () -> "email";
    }

    @Bean
    @Profile("!staging")
    NotificationService consoleNotificationService() {
        return () -> "console";
    }
}
