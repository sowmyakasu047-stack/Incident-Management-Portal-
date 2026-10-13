
package com.company.incidentportal.service;

import com.company.incidentportal.dto.IncidentRequest;
import com.company.incidentportal.dto.IncidentResponse;
import com.company.incidentportal.entity.Incident;
import com.company.incidentportal.entity.User;
import com.company.incidentportal.repository.IncidentRepository;
import com.company.incidentportal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
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

        LocalDateTime now = LocalDateTime.now();

        Incident incident = new Incident();
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setApplication(request.getApplication());
        incident.setPriority(request.getPriority());
        incident.setCategory(request.getCategory());
        incident.setStatus("OPEN");
        incident.setCreatedBy(createdBy);
        incident.setCreatedDate(now);
        incident.setUpdatedDate(now);
        incident.setDeleted(false);

        return toResponse(incidentRepository.save(incident));
    }

    public IncidentResponse getIncidentById(Long incidentId) {

        Incident incident = findActiveIncident(incidentId);
        return toResponse(incident);
    }

    public List<IncidentResponse> getAllIncidents() {

        return incidentRepository.findByDeletedFalse()
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
            incidents = incidentRepository.findByStatusAndDeletedFalse(status);
        } else if (priority != null && !priority.isBlank()) {
            incidents = incidentRepository.findByPriorityAndDeletedFalse(priority);
        } else if (application != null && !application.isBlank()) {
            incidents = incidentRepository.findByApplicationAndDeletedFalse(application);
        } else {
            incidents = incidentRepository.findByDeletedFalse();
        }

        return incidents.stream()
                .map(this::toResponse)
                .toList();
    }

    public IncidentResponse updateIncident(
            Long incidentId,
            IncidentRequest request) {

        Incident incident = findActiveIncident(incidentId);

        if ("CLOSED".equals(incident.getStatus())) {
            throw new RuntimeException("Closed incidents cannot be modified");
        }

        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setPriority(request.getPriority());
        incident.setCategory(request.getCategory());
        incident.setUpdatedDate(LocalDateTime.now());

        return toResponse(incidentRepository.save(incident));
    }

    public IncidentResponse assignIncident(
            Long incidentId,
            Long assignedToUserId) {

        Incident incident = findActiveIncident(incidentId);

        User assignedTo = userRepository.findById(assignedToUserId)
                .orElseThrow(() -> new RuntimeException("Assigned user not found"));

        incident.setAssignedTo(assignedTo);
        incident.setAssignedDate(LocalDateTime.now());
        incident.setUpdatedDate(LocalDateTime.now());

        return toResponse(incidentRepository.save(incident));
    }

    public IncidentResponse updateIncidentStatus(
            Long incidentId,
            String newStatus) {

        Incident incident = findActiveIncident(incidentId);
        String currentStatus = incident.getStatus();

        boolean validTransition =
                ("OPEN".equals(currentStatus)
                        && "IN_PROGRESS".equals(newStatus))
                || ("IN_PROGRESS".equals(currentStatus)
                        && "RESOLVED".equals(newStatus))
                || ("RESOLVED".equals(currentStatus)
                        && "CLOSED".equals(newStatus));

        if (!validTransition) {
            throw new RuntimeException(
                    "Invalid status transition: "
                            + currentStatus + " -> " + newStatus);
        }

        incident.setStatus(newStatus);
        incident.setUpdatedDate(LocalDateTime.now());

        return toResponse(incidentRepository.save(incident));
    }

    public void deleteIncident(Long incidentId) {

        Incident incident = findActiveIncident(incidentId);

        incident.setDeleted(true);
        incident.setUpdatedDate(LocalDateTime.now());

        incidentRepository.save(incident);
    }

    private Incident findActiveIncident(Long incidentId) {

        return incidentRepository.findByIdAndDeletedFalse(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));
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