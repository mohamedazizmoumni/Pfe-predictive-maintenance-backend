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
     * Paginated variant of the above - caps how many rows are pulled per
     * machine instead of loading its full notification history.
     */
    Page<Notification> findByMachineIdOrderByCreatedAtDesc(Long machineId, Pageable pageable);

    /**
     * Find all notifications targeting a specific role, ordered by most recent first.
     * Searches within comma-separated targetRoles field.
     *
     * @param role role name (e.g., "MANAGER")
     * @return list of notifications targeting the role
     */
    List<Notification> findByTargetRolesContainingOrderByCreatedAtDesc(String role);

    /**
     * Paginated variant of the above - this table grows with every
     * HIGH/CRITICAL prediction, so the role-filtered path needs the same cap
     * the unfiltered path already has.
     */
    Page<Notification> findByTargetRolesContainingOrderByCreatedAtDesc(String role, Pageable pageable);

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

    /**
     * Count unread notifications targeting a specific role — the role-scoped
     * counterpart to findByTargetRolesContainingOrderByCreatedAtDesc. Without
     * this, the header bell's badge (countByIsReadFalse, global) and its
     * panel list (role-filtered) could disagree: the badge would show a
     * nonzero count for a role that has zero notifications actually
     * addressed to it, making the panel look broken when clicked.
     */
    long countByIsReadFalseAndTargetRolesContaining(String role);
}
