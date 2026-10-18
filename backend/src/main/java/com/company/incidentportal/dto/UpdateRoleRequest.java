package com.company.incidentportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(
        @NotBlank @Pattern(regexp = "(?i)ADMIN|USER", message = "Role must be ADMIN or USER")
        String role) {
}
