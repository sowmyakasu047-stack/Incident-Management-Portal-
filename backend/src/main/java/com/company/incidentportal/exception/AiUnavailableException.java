package com.company.incidentportal.exception;

/** Thrown when the AI service is not configured, unreachable, or gives an unusable answer (HTTP 503). */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }
}
