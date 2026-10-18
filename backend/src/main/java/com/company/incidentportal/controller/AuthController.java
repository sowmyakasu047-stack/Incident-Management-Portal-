package com.company.incidentportal.controller;

import com.company.incidentportal.dto.LoginRequest;
import com.company.incidentportal.dto.LoginResponse;
import com.company.incidentportal.entity.User;
import com.company.incidentportal.exception.ResourceNotFoundException;
import com.company.incidentportal.security.JwtUtil;
import com.company.incidentportal.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for username '{}'", request.username());
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userService.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Successful login for user '{}'", user.getUsername());
        return new LoginResponse(
                jwtUtil.generateToken(user), user.getId(), user.getUsername(), user.getRole());
    }
}
