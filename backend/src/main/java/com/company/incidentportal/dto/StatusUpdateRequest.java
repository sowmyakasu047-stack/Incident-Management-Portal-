package com.company.incidentportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StatusUpdateRequest(
        @NotBlank
        @Pattern(regexp = "(?i)OPEN|IN_PROGRESS|RESOLVED|CLOSED",
                message = "Status must be OPEN, IN_PROGRESS, RESOLVED or CLOSED")
        String status) {
}
