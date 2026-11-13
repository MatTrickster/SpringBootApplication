package com.example.springboot.controller;

import java.util.Arrays;
import java.util.List;

import com.example.springboot.config.AppProperties;
import com.example.springboot.event.TradeEventHandler;
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
    private final TradeEventHandler tradeEventHandler;

    public record ConfigResponse(List<String> activeProfiles,
                                 String environmentName,
                                 boolean featureXEnabled,
                                 int maxPageSize,
                                 String supportEmail,
                                 String defaultUser,
                                 String notificationChannel,
                                 String tradeEventHandler) {
    }

    @GetMapping("/config")
    public ConfigResponse config() {
        return new ConfigResponse(
                effectiveProfiles(),
                appProperties.environmentName(),
                appProperties.featureXEnabled(),
                appProperties.maxPageSize(),
                appProperties.supportEmail(),
                appProperties.defaultUser(),
                notificationService.channel(),
                tradeEventHandler.name());
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
