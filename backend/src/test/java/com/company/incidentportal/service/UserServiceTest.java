package com.company.incidentportal.service;

import com.company.incidentportal.config.EmailPolicy;
import com.company.incidentportal.entity.User;
import com.company.incidentportal.exception.InvalidInputException;
import com.company.incidentportal.exception.InvalidOperationException;
import com.company.incidentportal.exception.ResourceNotFoundException;
import com.company.incidentportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, new EmailPolicy("aurum.com"));
    }

    @Test
    void shouldCreateUserWithHashedPassword() {
        when(userRepository.existsByUsername("carol")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("carol@aurum.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.createUser("carol", " Carol@Aurum.com ", "secret123", "user");

        assertEquals("HASH", saved.getPassword());
        assertEquals("carol@aurum.com", saved.getEmail());
        assertEquals("USER", saved.getRole());
        assertNotNull(saved.getCreatedDate());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(userRepository.existsByUsername("carol")).thenReturn(true);

        assertThrows(InvalidOperationException.class,
                () -> service.createUser("carol", "carol@aurum.com", "secret123", "USER"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectEmailOutsideCompanyDomain() {
        assertThrows(InvalidInputException.class,
                () -> service.createUser("carol", "carol@gmail.com", "secret123", "USER"));
        assertThrows(InvalidInputException.class,
                () -> service.createUser("carol", "not-an-email", "secret123", "USER"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByUsername("carol")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("carol@aurum.com")).thenReturn(true);

        assertThrows(InvalidOperationException.class,
                () -> service.createUser("carol", "carol@aurum.com", "secret123", "USER"));
    }

    @Test
    void shouldAllowMissingEmailForSeededAccounts() {
        when(userRepository.existsByUsername("seed")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(null, service.createUser("seed", " ", "secret123", "ADMIN").getEmail());
    }

    @Test
    void shouldRejectInvalidRole() {
        assertThrows(InvalidOperationException.class,
                () -> service.createUser("carol", "carol@aurum.com", "secret123", "SUPERUSER"));
        assertThrows(InvalidOperationException.class,
                () -> service.createUser("carol", "carol@aurum.com", "secret123", null));
    }

    @Test
    void shouldUpdateRole() {
        User user = new User();
        user.setUsername("bob");
        user.setRole("USER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertEquals("ADMIN", service.updateRole(5L, "admin", 1L).getRole());
    }

    @Test
    void shouldNotAllowChangingOwnRole() {
        assertThrows(InvalidOperationException.class, () -> service.updateRole(1L, "USER", 1L));
    }

    @Test
    void shouldFailUpdateRoleWhenUserMissing() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateRole(9L, "USER", 1L));
    }

    @Test
    void shouldDelegateFindAndSave() {
        User user = new User();
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertTrue(service.findByUsername("bob").isPresent());
        assertEquals(1, service.findAll().size());
        assertEquals(user, service.save(user));
    }
}
