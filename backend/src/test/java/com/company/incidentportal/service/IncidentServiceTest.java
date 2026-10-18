package com.company.incidentportal.service;

import com.company.incidentportal.dto.IncidentRequest;
import com.company.incidentportal.dto.IncidentResponse;
import com.company.incidentportal.entity.Incident;
import com.company.incidentportal.entity.Roles;
import com.company.incidentportal.entity.User;
import com.company.incidentportal.exception.ForbiddenException;
import com.company.incidentportal.exception.InvalidOperationException;
import com.company.incidentportal.exception.ResourceNotFoundException;
import com.company.incidentportal.repository.IncidentRepository;
import com.company.incidentportal.repository.UserRepository;
import com.company.incidentportal.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IncidentService service;

    private final AuthenticatedUser admin = new AuthenticatedUser(1L, "admin", Roles.ADMIN);
    private final AuthenticatedUser creatorUser = new AuthenticatedUser(2L, "alice", Roles.USER);
    private final AuthenticatedUser assigneeUser = new AuthenticatedUser(3L, "bob", Roles.USER);
    private final AuthenticatedUser stranger = new AuthenticatedUser(4L, "eve", Roles.USER);

    private User creator;
    private User assignee;

    @BeforeEach
    void setUp() {
        creator = user(2L, "alice", Roles.USER);
        assignee = user(3L, "bob", Roles.USER);
    }

    // ---------- create ----------

    @Test
    void shouldCreateIncidentWithOpenStatus() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(creator));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", 10L);
            return i;
        });

        IncidentResponse result = service.createIncident(request("Database Down", "high"), 2L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Database Down", result.getTitle());
        assertEquals("OPEN", result.getStatus());
        assertEquals("HIGH", result.getPriority());
        assertEquals("alice", result.getCreatedByUsername());
        assertNull(result.getAssignedTo());
    }

    @Test
    void shouldFailCreateWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createIncident(request("x", "LOW"), 99L));
        verify(incidentRepository, never()).save(any());
    }

    // ---------- read ----------

    @Test
    void shouldGetIncidentForAdmin() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "OPEN")));

        assertEquals(5L, service.getIncidentById(5L, admin).getId());
    }

    @Test
    void shouldGetIncidentForCreatorAndAssignee() {
        Incident incident = incident(5L, "OPEN");
        incident.setAssignedTo(assignee);
        when(incidentRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(incident));

        assertEquals(5L, service.getIncidentById(5L, creatorUser).getId());
        assertEquals(5L, service.getIncidentById(5L, assigneeUser).getId());
    }

    @Test
    void shouldForbidViewingForStranger() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "OPEN")));

        assertThrows(ForbiddenException.class, () -> service.getIncidentById(5L, stranger));
    }

    @Test
    void shouldThrowWhenIncidentNotFound() {
        when(incidentRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getIncidentById(404L, admin));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldListIncidentsWithResponseMapping() {
        Incident assigned = incident(1L, "OPEN");
        assigned.setAssignedTo(assignee);
        when(incidentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(assigned, incident(2L, "CLOSED")));

        List<IncidentResponse> adminView = service.getIncidents(null, null, null, admin);
        List<IncidentResponse> userView = service.getIncidents("OPEN", "HIGH", "Billing", creatorUser);

        assertEquals(2, adminView.size());
        assertEquals("bob", adminView.get(0).getAssignedToUsername());
        assertEquals(2, userView.size());
    }

    // ---------- update ----------

    @Test
    void shouldUpdateIncident() {
        Incident incident = incident(5L, "OPEN");
        when(incidentRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        IncidentResponse result = service.updateIncident(5L, request("New title", "critical"), creatorUser);

        assertEquals("New title", result.getTitle());
        assertEquals("CRITICAL", result.getPriority());
    }

    @Test
    void shouldAllowAdminAndAssigneeToUpdate() {
        Incident incident = incident(5L, "OPEN");
        incident.setAssignedTo(assignee);
        when(incidentRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        assertEquals("T1", service.updateIncident(5L, request("T1", "LOW"), admin).getTitle());
        assertEquals("T2", service.updateIncident(5L, request("T2", "LOW"), assigneeUser).getTitle());
    }

    @Test
    void shouldNotModifyClosedIncident() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "CLOSED")));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> service.updateIncident(5L, request("x", "LOW"), admin));
        assertEquals("Closed incidents cannot be modified", ex.getMessage());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void shouldForbidUpdateForStranger() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "OPEN")));

        assertThrows(ForbiddenException.class,
                () -> service.updateIncident(5L, request("x", "LOW"), stranger));
    }

    // ---------- assign ----------

    @Test
    void shouldAssignIncident() {
        Incident incident = incident(5L, "OPEN");
        when(incidentRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(3L)).thenReturn(Optional.of(assignee));
        when(incidentRepository.save(incident)).thenReturn(incident);

        IncidentResponse result = service.assignIncident(5L, 3L);

        assertEquals(3L, result.getAssignedTo());
        assertEquals("bob", result.getAssignedToUsername());
        assertNotNull(result.getAssignedDate());
    }

    @Test
    void shouldFailAssignWhenUserMissing() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "OPEN")));
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.assignIncident(5L, 77L));
    }

    @Test
    void shouldNotAssignClosedIncident() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "CLOSED")));

        assertThrows(InvalidOperationException.class, () -> service.assignIncident(5L, 3L));
    }

    // ---------- status ----------

    @Test
    void shouldWalkThroughFullLifecycleAsAdmin() {
        Incident incident = incident(5L, "OPEN");
        when(incidentRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        assertEquals("IN_PROGRESS", service.updateIncidentStatus(5L, "IN_PROGRESS", admin).getStatus());
        assertEquals("RESOLVED", service.updateIncidentStatus(5L, "resolved", admin).getStatus());
        assertEquals("CLOSED", service.updateIncidentStatus(5L, "CLOSED", admin).getStatus());
    }

    @Test
    void shouldAllowAssigneeToChangeStatus() {
        Incident incident = incident(5L, "OPEN");
        incident.setAssignedTo(assignee);
        when(incidentRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        assertEquals("IN_PROGRESS",
                service.updateIncidentStatus(5L, "IN_PROGRESS", assigneeUser).getStatus());
    }

    @Test
    void shouldAllowCreatorToCloseResolvedIncident() {
        Incident incident = incident(5L, "RESOLVED");
        when(incidentRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        assertEquals("CLOSED", service.updateIncidentStatus(5L, "CLOSED", creatorUser).getStatus());
    }

    @Test
    void shouldForbidCreatorFromOtherTransitions() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "OPEN")));

        assertThrows(ForbiddenException.class,
                () -> service.updateIncidentStatus(5L, "IN_PROGRESS", creatorUser));
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "OPEN")));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> service.updateIncidentStatus(5L, "CLOSED", admin));
        assertTrue(ex.getMessage().contains("OPEN -> CLOSED"));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTransitionOutOfClosed() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "CLOSED")));

        assertThrows(InvalidOperationException.class,
                () -> service.updateIncidentStatus(5L, "OPEN", admin));
    }

    @Test
    void shouldRejectUnknownOrBlankStatus() {
        when(incidentRepository.findByIdAndDeletedFalse(5L))
                .thenReturn(Optional.of(incident(5L, "OPEN")));

        assertThrows(InvalidOperationException.class,
                () -> service.updateIncidentStatus(5L, "DONE", admin));
        assertThrows(InvalidOperationException.class,
                () -> service.updateIncidentStatus(5L, " ", admin));
    }

    // ---------- delete ----------

    @Test
    void shouldSoftDeleteIncident() {
        Incident incident = incident(5L, "OPEN");
        when(incidentRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(incident));

        service.deleteIncident(5L);

        assertTrue(incident.isDeleted());
        verify(incidentRepository).save(incident);
    }

    @Test
    void shouldFailDeleteWhenNotFound() {
        when(incidentRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteIncident(404L));
    }

    // ---------- helpers ----------

    private IncidentRequest request(String title, String priority) {
        IncidentRequest r = new IncidentRequest();
        r.setTitle(title);
        r.setDescription("desc");
        r.setApplication("Billing");
        r.setPriority(priority);
        r.setCategory("Outage");
        return r;
    }

    private Incident incident(Long id, String status) {
        Incident i = new Incident();
        ReflectionTestUtils.setField(i, "id", id);
        i.setTitle("Title");
        i.setDescription("desc");
        i.setApplication("Billing");
        i.setPriority("HIGH");
        i.setCategory("Outage");
        i.setStatus(status);
        i.setCreatedBy(creator);
        i.setCreatedDate(LocalDateTime.now());
        i.setUpdatedDate(LocalDateTime.now());
        return i;
    }

    private User user(Long id, String username, String role) {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", id);
        u.setUsername(username);
        u.setRole(role);
        return u;
    }
}
