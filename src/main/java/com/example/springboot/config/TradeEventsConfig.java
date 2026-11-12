package com.example.springboot.config;

import com.example.springboot.entity.Trade;
import com.example.springboot.service.TradeEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ConditionalOnProperty: create a bean based on a property VALUE, not on the profile.
 *
 *   app.trade-events.enabled=true            -> logging publisher
 *   app.trade-events.enabled=false / missing -> no-op publisher (matchIfMissing = true)
 *
 * @Profile vs @ConditionalOnProperty:
 *  - @Profile ties behaviour to an environment name ("staging").
 *  - @ConditionalOnProperty ties it to a feature flag, which any environment
 *    (or a single test via @TestPropertySource) can flip without a new profile.
 *
 * The two conditions are written to be mutually exclusive, so exactly one bean
 * exists. (@ConditionalOnMissingBean is meant for auto-configuration classes;
 * in regular @Configuration it depends on bean registration order.)
 */
@Configuration
public class TradeEventsConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.trade-events", name = "enabled", havingValue = "true")
    TradeEventPublisher loggingTradeEventPublisher() {
        return new LoggingTradeEventPublisher();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.trade-events", name = "enabled", havingValue = "false",
            matchIfMissing = true)
    TradeEventPublisher noOpTradeEventPublisher() {
        return new NoOpTradeEventPublisher();
    }

    @Slf4j
    static class LoggingTradeEventPublisher implements TradeEventPublisher {
        @Override
        public void tradeCreated(Trade trade) {
            log.info("EVENT trade.created id={} user={} {} {}x{}",
                    trade.getId(), trade.getUserId(), trade.getType(), trade.getShares(), trade.getSymbol());
        }

        @Override
        public String name() {
            return "logging";
        }
    }

    static class NoOpTradeEventPublisher implements TradeEventPublisher {
        @Override
        public void tradeCreated(Trade trade) {
            // intentionally empty
        }

        @Override
        public String name() {
            return "noop";
        }
    }
}
