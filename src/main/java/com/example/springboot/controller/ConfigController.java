package com.example.springboot.controller;

import java.util.Arrays;
import java.util.List;

import com.example.springboot.config.AppProperties;
import com.example.springboot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConfigController {

    private final Environment environment;
    private final AppProperties appProperties;
    private final NotificationService notificationService;

    public record ConfigResponse(List<String> activeProfiles,
                                 String environmentName,
                                 boolean featureXEnabled,
                                 int maxPageSize,
                                 String supportEmail,
                                 String notificationChannel) {
    }

    @GetMapping("/config")
    public ConfigResponse config() {
        return new ConfigResponse(
                effectiveProfiles(),
                appProperties.environmentName(),
                appProperties.featureXEnabled(),
                appProperties.maxPageSize(),
                appProperties.supportEmail(),
                notificationService.channel());
    }

    /**
     * Gotcha: when no profile is explicitly activated, getActiveProfiles() is EMPTY
     * even though spring.profiles.default=dev is in effect. The default profiles
     * are exposed separately via getDefaultProfiles().
     */
    private List<String> effectiveProfiles() {
        String[] active = environment.getActiveProfiles();
        return Arrays.asList(active.length > 0 ? active : environment.getDefaultProfiles());
    }
}
