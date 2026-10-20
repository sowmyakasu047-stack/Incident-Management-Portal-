package com.company.incidentportal.config;

import com.company.incidentportal.entity.Roles;
import com.company.incidentportal.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin (and an optional demo user) on startup so you can log in.
 * Passwords come from configuration (env vars); a blank password means "do not seed".
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;
    private final String userUsername;
    private final String userEmail;
    private final String userPassword;

    public DataInitializer(UserService userService,
                           @Value("${app.seed.admin-username:admin}") String adminUsername,
                           @Value("${app.seed.admin-email:}") String adminEmail,
                           @Value("${app.seed.admin-password:}") String adminPassword,
                           @Value("${app.seed.user-username:user}") String userUsername,
                           @Value("${app.seed.user-email:}") String userEmail,
                           @Value("${app.seed.user-password:}") String userPassword) {
        this.userService = userService;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.userUsername = userUsername;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
    }

    @Override
    public void run(String... args) {
        seed(adminUsername, adminEmail, adminPassword, Roles.ADMIN);
        seed(userUsername, userEmail, userPassword, Roles.USER);
    }

    private void seed(String username, String email, String password, String role) {
        if (password == null || password.isBlank()) {
            log.info("No seed password configured for '{}'; skipping", username);
            return;
        }
        if (userService.findByUsername(username).isPresent()) {
            return;
        }
        userService.createUser(username, email, password, role);
        log.info("Seeded {} account '{}'", role, username);
    }
}
