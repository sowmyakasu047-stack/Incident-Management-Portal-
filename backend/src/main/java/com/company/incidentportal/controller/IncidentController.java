package com.company.incidentportal.controller;

import com.company.incidentportal.dto.IncidentRequest;
import com.company.incidentportal.dto.IncidentResponse;
import com.company.incidentportal.dto.StatusUpdateRequest;
import com.company.incidentportal.security.AuthenticatedUser;
import com.company.incidentportal.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // Create a new incident owned by the logged-in user
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse createIncident(
            @Valid @RequestBody IncidentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return incidentService.createIncident(request, user.id());
    }

    // List active incidents (admin: all, user: own), optionally filtered
    @GetMapping
    public List<IncidentResponse> getIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String application,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return incidentService.getIncidents(status, priority, application, user);
    }

    // Retrieve one active incident by ID
    @GetMapping("/{id}")
    public IncidentResponse getIncidentById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return incidentService.getIncidentById(id, user);
    }

    // Update an existing incident
    @PutMapping("/{id}")
    public IncidentResponse updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody IncidentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return incidentService.updateIncident(id, request, user);
    }

    // Assign an incident to a user (admin only)
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse assignIncident(
            @PathVariable Long id,
            @RequestParam Long userId) {

        return incidentService.assignIncident(id, userId);
    }

    // Change status (admin or assignee; creator may close a resolved incident)
    @PutMapping("/{id}/status")
    public IncidentResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return incidentService.updateIncidentStatus(id, request.status(), user);
    }

    // Soft delete (admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIncident(@PathVariable Long id) {
        incidentService.deleteIncident(id);
    }
}
