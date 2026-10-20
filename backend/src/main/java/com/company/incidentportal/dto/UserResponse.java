package com.company.incidentportal.dto;

import com.company.incidentportal.entity.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String email, String role, LocalDateTime createdDate) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getCreatedDate());
    }
}
