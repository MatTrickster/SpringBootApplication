package com.example.springboot.exception;

import java.time.Duration;

/** Work did not finish within its time budget. Mapped to 504 Gateway Timeout. */
public class OperationTimeoutException extends RuntimeException {

    public OperationTimeoutException(String operation, Duration timeout, Throwable cause) {
        super(operation + " did not complete within " + timeout.toMillis() + " ms", cause);
    }
}
