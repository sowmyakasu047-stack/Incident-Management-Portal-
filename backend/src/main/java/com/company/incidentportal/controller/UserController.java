package com.company.incidentportal.controller;

import com.company.incidentportal.dto.CreateUserRequest;
import com.company.incidentportal.dto.UpdateRoleRequest;
import com.company.incidentportal.dto.UserResponse;
import com.company.incidentportal.security.AuthenticatedUser;
import com.company.incidentportal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** User management. Access is restricted to the ADMIN role in SecurityConfig. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getUsers() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(
                userService.createUser(
                        request.username(), request.email(), request.password(), request.role()));
    }

    @PutMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable Long id,
                                   @Valid @RequestBody UpdateRoleRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return UserResponse.from(userService.updateRole(id, request.role(), actor.id()));
    }
}
