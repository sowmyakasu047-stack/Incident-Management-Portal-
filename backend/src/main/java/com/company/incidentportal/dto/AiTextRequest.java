package com.company.incidentportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Draft incident text sent to the AI helpers (same limits as a real incident). */
public record AiTextRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 1000) String description) {
}
