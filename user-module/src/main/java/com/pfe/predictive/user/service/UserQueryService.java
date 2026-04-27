package com.pfe.predictive.user.service;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.core.entity.UserStatus;
import com.pfe.predictive.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UserQueryService - User management read operations.
 * Handles: fetching, filtering, searching, statistics
 *
 * @author User Module
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    // ============================================================================
    // SINGLE FETCH OPERATIONS
    // ============================================================================

    /**
     * Get user by ID
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }

    // ============================================================================
    // LIST & FILTER OPERATIONS
    // ============================================================================

    /**
     * Get all users with pagination
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Get users by status
     *
     * @param status ACTIVE, INACTIVE, or LOCKED
     * @param pageable pagination
     * @return paginated users
     */
    public Page<User> getUsersByStatus(UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }

    /**
     * Search users by name (first or last)
     */
    public Page<User> searchByName(String name, Pageable pageable) {
        return userRepository.searchByName(name, pageable);
    }

    /**
     * Get users by department
     */
    public Page<User> getUsersByDepartment(String department, Pageable pageable) {
        return userRepository.findByDepartment(department, pageable);
    }

    /**
     * Get users with specific role
     */
    public Page<User> getUsersByRole(String role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    /**
     * Get active users in department
     */
    public Page<User> getActiveUsersByDepartment(String department, Pageable pageable) {
        return userRepository.findByDepartmentAndStatus(department, UserStatus.ACTIVE, pageable);
    }

    // ============================================================================
    // SPECIFIC QUERIES
    // ============================================================================

    /**
     * Get all active users
     */
    public Page<User> getActiveUsers(Pageable pageable) {
        return userRepository.findByStatus(UserStatus.ACTIVE, pageable);
    }

    /**
     * Get all inactive users
     */
    public Page<User> getInactiveUsers(Pageable pageable) {
        return userRepository.findByStatus(UserStatus.INACTIVE, pageable);
    }

    /**
     * Get all locked users
     */
    public Page<User> getLockedUsers(Pageable pageable) {
        return userRepository.findByStatus(UserStatus.LOCKED, pageable);
    }

    /**
     * Get all locked accounts (non-paginated for admin actions)
     */
    public List<User> getAllLockedAccounts() {
        return userRepository.findByStatusOrderByLockedDateDesc(UserStatus.LOCKED);
    }

    /**
     * Get all admin users
     */
    public List<User> getAllAdmins() {
        return userRepository.findAllAdmins();
    }

    /**
     * Get users who logged in recently
     */
    public List<User> getRecentlyActiveUsers(int daysBack) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysBack);
        return userRepository.findByLastLoginDateAfterOrderByLastLoginDateDesc(cutoffDate);
    }

    // ============================================================================
    // CHECK/COUNT OPERATIONS
    // ============================================================================

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Count users by status
     */
    public long countByStatus(UserStatus status) {
        return userRepository.countByStatus(status);
    }

    /**
     * Get total user count
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }

    // ============================================================================
    // STATISTICS
    // ============================================================================

    /**
     * Get user management statistics
     */
    public UserManagementStats getUserStats() {
        long totalUsers = getTotalUserCount();
        long activeUsers = countByStatus(UserStatus.ACTIVE);
        long inactiveUsers = countByStatus(UserStatus.INACTIVE);
        long lockedUsers = countByStatus(UserStatus.LOCKED);

        List<User> admins = getAllAdmins();
        long adminCount = admins.size();

        return UserManagementStats.builder()
            .totalUsers(totalUsers)
            .activeUsers(activeUsers)
            .inactiveUsers(inactiveUsers)
            .lockedUsers(lockedUsers)
            .adminCount(adminCount)
            .build();
    }

    // ============================================================================
    // HELPER CLASSES
    // ============================================================================

    /**
     * User management statistics data class
     */
    public static class UserManagementStats {
        public Long totalUsers;
        public Long activeUsers;
        public Long inactiveUsers;
        public Long lockedUsers;
        public Long adminCount;

        public static UserManagementStatsBuilder builder() {
            return new UserManagementStatsBuilder();
        }

        public static class UserManagementStatsBuilder {
            private Long totalUsers;
            private Long activeUsers;
            private Long inactiveUsers;
            private Long lockedUsers;
            private Long adminCount;

            public UserManagementStatsBuilder totalUsers(Long total) {
                this.totalUsers = total;
                return this;
            }

            public UserManagementStatsBuilder activeUsers(Long active) {
                this.activeUsers = active;
                return this;
            }

            public UserManagementStatsBuilder inactiveUsers(Long inactive) {
                this.inactiveUsers = inactive;
                return this;
            }

            public UserManagementStatsBuilder lockedUsers(Long locked) {
                this.lockedUsers = locked;
                return this;
            }

            public UserManagementStatsBuilder adminCount(Long count) {
                this.adminCount = count;
                return this;
            }

            public UserManagementStats build() {
                UserManagementStats stats = new UserManagementStats();
                stats.totalUsers = this.totalUsers;
                stats.activeUsers = this.activeUsers;
                stats.inactiveUsers = this.inactiveUsers;
                stats.lockedUsers = this.lockedUsers;
                stats.adminCount = this.adminCount;
                return stats;
            }
        }
    }
}
