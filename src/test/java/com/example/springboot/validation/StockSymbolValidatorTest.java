package com.example.springboot.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests the custom constraint through a real Validator, no Spring needed. */
class StockSymbolValidatorTest {

    record Holder(@StockSymbol String symbol) {
    }

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "ABX", "AAPL", "GOOGL"})
    void validSymbols(String symbol) {
        assertThat(validator.validate(new Holder(symbol))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abx", "TOOLONG", "AB1", "A B", "ÄBC"})
    void invalidSymbols(String symbol) {
        assertThat(validator.validate(new Holder(symbol))).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void nullIsValid_presenceIsNotBlanksJob() {
        assertThat(validator.validate(new Holder(null))).isEmpty();
    }
}
