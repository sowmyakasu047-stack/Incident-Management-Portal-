package com.company.incidentportal.entity;

import com.company.incidentportal.exception.InvalidOperationException;

/** Incident lifecycle: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED. */
public enum IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(IncidentStatus next) {
        return switch (this) {
            case OPEN -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == RESOLVED;
            case RESOLVED -> next == CLOSED;
            case CLOSED -> false;
        };
    }

    public static IncidentStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidOperationException("Status is required");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOperationException("Unknown status: " + value);
        }
    }
}
