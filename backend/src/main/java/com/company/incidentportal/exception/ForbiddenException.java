package com.company.incidentportal.exception;

/** Thrown when the authenticated user is not allowed to perform an action (HTTP 403). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
