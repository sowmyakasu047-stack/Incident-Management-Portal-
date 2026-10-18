package com.company.incidentportal.security;

import com.company.incidentportal.entity.Roles;

/** The caller identity extracted from a validated JWT; available via @AuthenticationPrincipal. */
public record AuthenticatedUser(Long id, String username, String role) {

    public boolean isAdmin() {
        return Roles.ADMIN.equals(role);
    }
}
