package com.company.incidentportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        String username,
        @NotBlank @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        String password,
        @NotBlank @Pattern(regexp = "(?i)ADMIN|USER", message = "Role must be ADMIN or USER")
        String role) {
}
