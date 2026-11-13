package com.example.springboot.config;

import com.example.springboot.event.TradeCreatedEvent;
import com.example.springboot.event.TradeEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ConditionalOnProperty: create a bean based on a property VALUE, not on the profile.
 *
 *   app.trade-events.enabled=true            -> logging handler
 *   app.trade-events.enabled=false / missing -> no-op handler (matchIfMissing = true)
 *
 * @Profile vs @ConditionalOnProperty:
 *  - @Profile ties behaviour to an environment name ("staging").
 *  - @ConditionalOnProperty ties it to a feature flag, which any environment
 *    (or a single test via @SpringBootTest(properties = ...)) can flip without a new profile.
 *
 * The two conditions are mutually exclusive, so exactly one bean exists.
 * (@ConditionalOnMissingBean is meant for auto-configuration classes;
 * in regular @Configuration it depends on bean registration order.)
 */
@Configuration
public class TradeEventsConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.trade-events", name = "enabled", havingValue = "true")
    TradeEventHandler loggingTradeEventHandler() {
        return new LoggingTradeEventHandler();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.trade-events", name = "enabled", havingValue = "false",
            matchIfMissing = true)
    TradeEventHandler noOpTradeEventHandler() {
        return new NoOpTradeEventHandler();
    }

    @Slf4j
    static class LoggingTradeEventHandler implements TradeEventHandler {
        @Override
        public void handle(TradeCreatedEvent event) {
            log.info("EVENT trade.created id={} user={} {} {}x{}", event.tradeId(), event.userId(),
                    event.type(), event.shares(), event.symbol());
        }

        @Override
        public String name() {
            return "logging";
        }
    }

    static class NoOpTradeEventHandler implements TradeEventHandler {
        @Override
        public void handle(TradeCreatedEvent event) {
            // intentionally empty
        }

        @Override
        public String name() {
            return "noop";
        }
    }
}
