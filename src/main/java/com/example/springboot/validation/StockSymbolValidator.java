package com.example.springboot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class StockSymbolValidator implements ConstraintValidator<StockSymbol, String> {

    private static final Pattern SYMBOL = Pattern.compile("[A-Z]{1,5}");

    /**
     * Convention: null is VALID. Each constraint checks one thing, and
     * "is it present?" is @NotNull/@NotBlank's job. That keeps them composable.
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || SYMBOL.matcher(value).matches();
    }
}
