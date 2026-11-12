package com.example.springboot.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Bean Validation constraint: a ticker of 1-5 uppercase letters (ABX, AAPL).
 *
 * The three members message/groups/payload are REQUIRED by the spec; validation
 * fails at runtime with ConstraintDefinitionException if one is missing.
 */
@Documented
@Constraint(validatedBy = StockSymbolValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StockSymbol {

    String message() default "must be 1-5 uppercase letters, e.g. ABX";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
