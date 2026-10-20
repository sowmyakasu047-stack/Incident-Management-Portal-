package com.company.incidentportal.exception;

/** Thrown when supplied data is not acceptable, e.g. an email outside the company domain (HTTP 400). */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
