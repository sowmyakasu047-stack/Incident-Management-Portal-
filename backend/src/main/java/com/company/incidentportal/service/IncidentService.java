package com.company.incidentportal.service;

import com.company.incidentportal.dto.IncidentRequest;
import com.company.incidentportal.dto.IncidentResponse;
import com.company.incidentportal.entity.Incident;
import com.company.incidentportal.entity.IncidentStatus;
import com.company.incidentportal.entity.User;
import com.company.incidentportal.exception.ForbiddenException;
import com.company.incidentportal.exception.InvalidOperationException;
import com.company.incidentportal.exception.ResourceNotFoundException;
import com.company.incidentportal.repository.IncidentRepository;
import com.company.incidentportal.repository.IncidentSpecifications;
import com.company.incidentportal.repository.UserRepository;
import com.company.incidentportal.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public IncidentService(
            IncidentRepository incidentRepository,
            UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    public IncidentResponse createIncident(
            IncidentRequest request,
            Long createdByUserId) {

        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDateTime now = LocalDateTime.now();

        Incident incident = new Incident();
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setApplication(request.getApplication());
        incident.setPriority(request.getPriority().trim().toUpperCase());
        incident.setCategory(request.getCategory());
        incident.setStatus(IncidentStatus.OPEN.name());
        incident.setCreatedBy(createdBy);
        incident.setCreatedDate(now);
        incident.setUpdatedDate(now);
        incident.setDeleted(false);

        Incident saved = incidentRepository.save(incident);
        log.info("Incident {} created by user '{}' (priority {})",
                saved.getId(), createdBy.getUsername(), saved.getPriority());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long incidentId, AuthenticatedUser actor) {

        Incident incident = findActiveIncident(incidentId);
        if (!actor.isAdmin() && !isParticipant(incident, actor)) {
            throw new ForbiddenException("User " + actor.username()
                    + " may not view incident " + incidentId);
        }
        return toResponse(incident);
    }

    /** Admins see every incident; other users see incidents they created or are assigned to. */
    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidents(
            String status,
            String priority,
            String application,
            AuthenticatedUser actor) {

        Long participantId = actor.isAdmin() ? null : actor.id();

        return incidentRepository
                .findAll(IncidentSpecifications.filter(status, priority, application, participantId),
                        Sort.by(Sort.Direction.DESC, "createdDate"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public IncidentResponse updateIncident(
            Long incidentId,
            IncidentRequest request,
            AuthenticatedUser actor) {

        Incident incident = findActiveIncident(incidentId);

        if (!actor.isAdmin() && !isParticipant(incident, actor)) {
            throw new ForbiddenException("User " + actor.username()
                    + " may not modify incident " + incidentId);
        }
        if (IncidentStatus.CLOSED.name().equals(incident.getStatus())) {
            throw new InvalidOperationException("Closed incidents cannot be modified");
        }

        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setPriority(request.getPriority().trim().toUpperCase());
        incident.setCategory(request.getCategory());
        incident.setUpdatedDate(LocalDateTime.now());

        log.info("Incident {} updated by user '{}'", incidentId, actor.username());
        return toResponse(incidentRepository.save(incident));
    }

    public IncidentResponse assignIncident(
            Long incidentId,
            Long assignedToUserId) {

        Incident incident = findActiveIncident(incidentId);

        if (IncidentStatus.CLOSED.name().equals(incident.getStatus())) {
            throw new InvalidOperationException("Closed incidents cannot be reassigned");
        }

        User assignedTo = userRepository.findById(assignedToUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found"));

        LocalDateTime now = LocalDateTime.now();
        incident.setAssignedTo(assignedTo);
        incident.setAssignedDate(now);
        incident.setUpdatedDate(now);

        log.info("Incident {} assigned to user '{}'", incidentId, assignedTo.getUsername());
        return toResponse(incidentRepository.save(incident));
    }

    /**
     * Admin or assignee may move an incident along its lifecycle; the creator may only close an
     * incident that has already been RESOLVED.
     */
    public IncidentResponse updateIncidentStatus(
            Long incidentId,
            String newStatus,
            AuthenticatedUser actor) {

        Incident incident = findActiveIncident(incidentId);
        IncidentStatus current = IncidentStatus.parse(incident.getStatus());
        IncidentStatus next = IncidentStatus.parse(newStatus);

        boolean assignee = isAssignee(incident, actor);
        boolean creatorClosing = isCreator(incident, actor)
                && current == IncidentStatus.RESOLVED
                && next == IncidentStatus.CLOSED;

        if (!actor.isAdmin() && !assignee && !creatorClosing) {
            throw new ForbiddenException("User " + actor.username()
                    + " may not change the status of incident " + incidentId);
        }

        if (!current.canTransitionTo(next)) {
            throw new InvalidOperationException(
                    "Invalid status transition: " + current + " -> " + next);
        }

        incident.setStatus(next.name());
        incident.setUpdatedDate(LocalDateTime.now());

        log.info("Incident {} status {} -> {} by user '{}'",
                incidentId, current, next, actor.username());
        return toResponse(incidentRepository.save(incident));
    }

    /** Soft delete: the row stays in the database but is hidden from every query. */
    public void deleteIncident(Long incidentId) {

        Incident incident = findActiveIncident(incidentId);

        incident.setDeleted(true);
        incident.setUpdatedDate(LocalDateTime.now());

        incidentRepository.save(incident);
        log.info("Incident {} soft-deleted", incidentId);
    }

    private Incident findActiveIncident(Long incidentId) {

        return incidentRepository.findByIdAndDeletedFalse(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
    }

    private boolean isCreator(Incident incident, AuthenticatedUser actor) {
        return incident.getCreatedBy() != null
                && Objects.equals(incident.getCreatedBy().getId(), actor.id());
    }

    private boolean isAssignee(Incident incident, AuthenticatedUser actor) {
        return incident.getAssignedTo() != null
                && Objects.equals(incident.getAssignedTo().getId(), actor.id());
    }

    private boolean isParticipant(Incident incident, AuthenticatedUser actor) {
        return isCreator(incident, actor) || isAssignee(incident, actor);
    }

    private IncidentResponse toResponse(Incident incident) {

        IncidentResponse response = new IncidentResponse();

        response.setId(incident.getId());
        response.setTitle(incident.getTitle());
        response.setDescription(incident.getDescription());
        response.setApplication(incident.getApplication());
        response.setPriority(incident.getPriority());
        response.setStatus(incident.getStatus());
        response.setCategory(incident.getCategory());

        if (incident.getAssignedTo() != null) {
            response.setAssignedTo(incident.getAssignedTo().getId());
            response.setAssignedToUsername(incident.getAssignedTo().getUsername());
        }

        response.setAssignedDate(incident.getAssignedDate());
        response.setCreatedBy(incident.getCreatedBy().getId());
        response.setCreatedByUsername(incident.getCreatedBy().getUsername());
        response.setCreatedDate(incident.getCreatedDate());
        response.setUpdatedDate(incident.getUpdatedDate());

        return response;
    }
}
