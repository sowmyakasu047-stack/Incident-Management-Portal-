package com.company.incidentportal.repository;

import com.company.incidentportal.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByCreatedById(Long userId);

    List<Incident> findByStatus(String status);

    List<Incident> findByPriority(String priority);

    List<Incident> findByApplication(String application);

    List<Incident> findByAssignedToId(Long userId);
}
