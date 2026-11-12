package com.example.springboot.exception;

/** Business rule violation: the resource already exists. Mapped to 409 Conflict. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resource, String field, Object value) {
        super(resource + " with " + field + " '" + value + "' already exists");
    }
}
