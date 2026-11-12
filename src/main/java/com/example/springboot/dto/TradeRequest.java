package com.example.springboot.dto;

import com.example.springboot.entity.Trade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Incoming payload for POST /trades. Keeping the request DTO separate from the
 * entity means clients cannot set "id" and validation rules live in one place.
 */
public record TradeRequest(
        @NotBlank @Pattern(regexp = "buy|sell", message = "must be 'buy' or 'sell'") String type,
        @NotNull @Positive Integer userId,
        @NotBlank String symbol,
        @NotNull @Min(1) @Max(100) Integer shares,
        @NotNull @Positive Integer price,
        @NotNull Long timestamp) {

    public Trade toEntity() {
        return Trade.builder()
                .type(type)
                .userId(userId)
                .symbol(symbol)
                .shares(shares)
                .price(price)
                .timestamp(timestamp)
                .build();
    }
}
