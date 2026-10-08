package com.company.incidentportal.service;

import com.company.incidentportal.dto.IncidentRequest;
import com.company.incidentportal.dto.IncidentResponse;
import com.company.incidentportal.entity.Incident;
import com.company.incidentportal.entity.User;
import com.company.incidentportal.repository.IncidentRepository;
import com.company.incidentportal.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

@Service
public class IncidentService {

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
                .orElseThrow(() -> new RuntimeException("User not found"));

        Incident incident = new Incident();

        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setApplication(request.getApplication());
        incident.setPriority(request.getPriority());
        incident.setCategory(request.getCategory());

        // Every new incident starts as OPEN.
        incident.setStatus("OPEN");

        incident.setCreatedBy(createdBy);
        incident.setCreatedDate(LocalDateTime.now());
        incident.setUpdatedDate(LocalDateTime.now());

        Incident savedIncident = incidentRepository.save(incident);

        return toResponse(savedIncident);
    }

    public IncidentResponse getIncidentById(Long incidentId) {

    Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new RuntimeException("Incident not found"));

    return toResponse(incident);
    }
    
    public List<IncidentResponse> getAllIncidents() {

    return incidentRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<IncidentResponse> getIncidents(
        String status,
        String priority,
        String application) {

    List<Incident> incidents;

    if (status != null && !status.isBlank()) {
        incidents = incidentRepository.findByStatus(status);
    } else if (priority != null && !priority.isBlank()) {
        incidents = incidentRepository.findByPriority(priority);
    } else if (application != null && !application.isBlank()) {
        incidents = incidentRepository.findByApplication(application);
    } else {
        incidents = incidentRepository.findAll();
    }

    return incidents.stream()
            .map(this::toResponse)
            .toList();
    }
    
    public IncidentResponse updateIncident(
        Long incidentId,
        IncidentRequest request) {

    Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new RuntimeException("Incident not found"));

    if ("CLOSED".equals(incident.getStatus())) {
        throw new RuntimeException("Closed incidents cannot be modified");
    }

    incident.setTitle(request.getTitle());
    incident.setDescription(request.getDescription());
    incident.setPriority(request.getPriority());
    incident.setCategory(request.getCategory());
    incident.setUpdatedDate(LocalDateTime.now());

    Incident updatedIncident = incidentRepository.save(incident);

    return toResponse(updatedIncident);
    }

    public IncidentResponse assignIncident(
        Long incidentId,
        Long assignedToUserId) {

    Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new RuntimeException("Incident not found"));

    User assignedTo = userRepository.findById(assignedToUserId)
            .orElseThrow(() -> new RuntimeException("Assigned user not found"));

    incident.setAssignedTo(assignedTo);
    incident.setAssignedDate(LocalDateTime.now());
    incident.setUpdatedDate(LocalDateTime.now());

    Incident updatedIncident = incidentRepository.save(incident);

    return toResponse(updatedIncident);
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
        }
        
        response.setAssignedDate(incident.getAssignedDate());
        response.setCreatedBy(incident.getCreatedBy().getId());
        response.setCreatedDate(incident.getCreatedDate());
        response.setUpdatedDate(incident.getUpdatedDate());

        return response;
    }
}