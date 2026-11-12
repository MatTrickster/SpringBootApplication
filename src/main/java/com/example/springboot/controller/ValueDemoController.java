package com.example.springboot.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Value: inject single properties directly. Compare with config/AppProperties.
 *
 *   ${key}            required: startup FAILS if the key is missing
 *   ${key:fallback}   default value if the key is missing
 *   String[]/List     comma-separated values are split automatically
 *   #{...}            SpEL expression (not available with @ConfigurationProperties)
 *
 * Note the explicit constructor: Lombok's @RequiredArgsConstructor does NOT copy
 * @Value onto constructor parameters (unless configured in lombok.config).
 */
@RestController
public class ValueDemoController {

    public record ValueDemoResponse(String applicationName,
                                    int maxPageSize,
                                    boolean featureXEnabled,
                                    String missingWithDefault,
                                    List<String> allowedSymbols,
                                    int maxPageSizeDoubled) {
    }

    private final ValueDemoResponse response;

    public ValueDemoController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${app.max-page-size}") int maxPageSize,             // String -> int conversion
            @Value("${app.feature-x-enabled}") boolean featureXEnabled,
            @Value("${app.not-defined-anywhere:fallback-value}") String missingWithDefault,
            @Value("${app.allowed-symbols:ABX,AAPL,MSFT}") String[] allowedSymbols,
            @Value("#{${app.max-page-size} * 2}") int maxPageSizeDoubled) {   // SpEL
        this.response = new ValueDemoResponse(applicationName, maxPageSize, featureXEnabled,
                missingWithDefault, List.of(allowedSymbols), maxPageSizeDoubled);
    }

    @GetMapping("/config/value-demo")
    public ValueDemoResponse valueDemo() {
        return response;
    }
}
