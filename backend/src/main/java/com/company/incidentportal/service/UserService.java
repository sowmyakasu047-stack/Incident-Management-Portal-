package com.company.incidentportal.service;

import com.company.incidentportal.entity.Roles;
import com.company.incidentportal.entity.User;
import com.company.incidentportal.exception.InvalidOperationException;
import com.company.incidentportal.exception.ResourceNotFoundException;
import com.company.incidentportal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /** Creates a user whose password is stored as a BCrypt hash. */
    public User createUser(String username, String rawPassword, String role) {

        String normalizedRole = normalizeRole(role);

        if (userRepository.existsByUsername(username)) {
            throw new InvalidOperationException("Username already exists: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(normalizedRole);
        user.setCreatedDate(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("User '{}' created with role {}", saved.getUsername(), saved.getRole());
        return saved;
    }

    /** Admins cannot change their own role, which prevents locking everyone out of admin. */
    public User updateRole(Long userId, String role, Long actingUserId) {

        String normalizedRole = normalizeRole(role);

        if (userId.equals(actingUserId)) {
            throw new InvalidOperationException("You cannot change your own role");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(normalizedRole);

        log.info("User '{}' role changed to {}", user.getUsername(), normalizedRole);
        return userRepository.save(user);
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if (!Roles.ADMIN.equals(normalized) && !Roles.USER.equals(normalized)) {
            throw new InvalidOperationException("Role must be ADMIN or USER");
        }
        return normalized;
    }
}
