package com.company.incidentportal.exception;

/** Thrown when a business rule is violated, e.g. an invalid status transition (HTTP 409). */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
