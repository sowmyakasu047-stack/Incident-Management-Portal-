package com.company.incidentportal.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Uniform error body returned by every failing API call. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, null);
    }

    public static ErrorResponse of(int status, String error, String message,
                                   Map<String, String> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, fieldErrors);
    }
}
