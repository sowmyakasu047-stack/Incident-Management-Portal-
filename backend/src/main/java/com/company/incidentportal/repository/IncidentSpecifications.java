package com.company.incidentportal.repository;

import com.company.incidentportal.entity.Incident;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Builds the dynamic "list incidents" query: optional filters are combined with AND. */
public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    /**
     * @param participantId when not null, restricts results to incidents created by or assigned
     *                      to that user (used for non-admin callers)
     */
    public static Specification<Incident> filter(String status, String priority,
                                                 String application, Long participantId) {
        return (root, query, cb) -> {
            // Avoid N+1 queries for usernames; skip fetch joins for count queries.
            if (query != null && !Long.class.equals(query.getResultType())
                    && !long.class.equals(query.getResultType())) {
                root.fetch("createdBy", JoinType.INNER);
                root.fetch("assignedTo", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            }
            if (hasText(priority)) {
                predicates.add(cb.equal(root.get("priority"), priority.trim().toUpperCase()));
            }
            if (hasText(application)) {
                predicates.add(cb.equal(cb.lower(root.get("application")),
                        application.trim().toLowerCase()));
            }
            if (participantId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("createdBy").get("id"), participantId),
                        cb.equal(root.get("assignedTo").get("id"), participantId)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
