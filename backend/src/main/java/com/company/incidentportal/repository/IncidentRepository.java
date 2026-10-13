
package com.company.incidentportal.repository;

import com.company.incidentportal.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // Retrieve only active incidents.
    List<Incident> findByDeletedFalse();

    Optional<Incident> findByIdAndDeletedFalse(Long id);

    // Filter active incidents.
    List<Incident> findByStatusAndDeletedFalse(String status);

    List<Incident> findByPriorityAndDeletedFalse(String priority);

    List<Incident> findByApplicationAndDeletedFalse(String application);

    // Other useful queries for active incidents.
    List<Incident> findByCreatedByIdAndDeletedFalse(Long userId);

    List<Incident> findByAssignedToIdAndDeletedFalse(Long userId);
}