package com.company.incidentportal.dto;

public record LoginResponse(String token, Long userId, String username, String role) {
}
