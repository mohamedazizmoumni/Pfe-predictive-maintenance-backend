package com.pfe.predictive.notification.repository;

import com.pfe.predictive.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Notification entity.
 * Handles persistence and querying of machine risk notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all unread notifications, ordered by most recent first.
     *
     * @return list of unread notifications
     */
    List<Notification> findByIsReadFalseOrderByCreatedAtDesc();

    /**
     * Find all notifications for a specific machine, ordered by most recent first.
     *
     * @param machineId ID of the machine
     * @return list of notifications for the machine
     */
    List<Notification> findByMachineIdOrderByCreatedAtDesc(Long machineId);

    /**
     * Find all notifications targeting a specific role, ordered by most recent first.
     * Searches within comma-separated targetRoles field.
     *
     * @param role role name (e.g., "MANAGER")
     * @return list of notifications targeting the role
     */
    List<Notification> findByTargetRolesContainingOrderByCreatedAtDesc(String role);

    /**
     * Find all notifications with pagination, ordered by most recent first.
     *
     * @param pageable pagination parameters
     * @return page of notifications
     */
    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Count the number of unread notifications.
     *
     * @return number of unread notifications
     */
    long countByIsReadFalse();
}
