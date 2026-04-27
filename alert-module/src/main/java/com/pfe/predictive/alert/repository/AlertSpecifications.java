package com.pfe.predictive.alert.repository;

import com.pfe.predictive.alert.dto.AlertSearchCriteria;
import com.pfe.predictive.alert.entity.Alert;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Factory for {@link Specification} instances used to filter alerts dynamically.
 */
public final class AlertSpecifications {

    private AlertSpecifications() {
    }

    public static Specification<Alert> withFilters(AlertSearchCriteria criteria) {
        return (root, query, builder) -> {
            if (criteria == null) {
                return builder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getSeverity() != null) {
                predicates.add(builder.equal(root.get("severity"), criteria.getSeverity()));
            }

            if (criteria.getViewed() != null) {
                predicates.add(builder.equal(root.get("viewed"), criteria.getViewed()));
            }

            if (StringUtils.hasText(criteria.getAssignedTo())) {
                String like = "%" + criteria.getAssignedTo().trim().toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.get("assignedTo")), like));
            }

            if (StringUtils.hasText(criteria.getSearch())) {
                String like = "%" + criteria.getSearch().trim().toLowerCase() + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("title")), like),
                    builder.like(builder.lower(root.get("message")), like),
                    builder.like(builder.lower(root.get("recommendations")), like),
                    builder.like(builder.lower(root.get("resolutionNotes")), like),
                    builder.like(builder.lower(root.get("sourceReference")), like),
                    builder.like(builder.lower(root.get("assignedTo")), like)
                ));
            }

            return predicates.isEmpty()
                ? builder.conjunction()
                : builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
