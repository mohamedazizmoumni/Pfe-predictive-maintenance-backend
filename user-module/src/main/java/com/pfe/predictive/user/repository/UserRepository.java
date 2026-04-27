package com.pfe.predictive.user.repository;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.core.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserRepository - Spring Data JPA for User entity.
 * Provides CRUD and custom query methods for user management.
 *
 * @author User Module
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by unique username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find users by status (ACTIVE, INACTIVE, LOCKED)
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * Find users by partial name match
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%',:name,'%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:name,'%'))")
    Page<User> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Find users by department
     */
    Page<User> findByDepartment(String department, Pageable pageable);

    /**
     * Find users with specific role
     */
    @Query("SELECT u FROM User u WHERE u.rolesList LIKE CONCAT('%',:role,'%')")
    Page<User> findByRole(@Param("role") String role, Pageable pageable);

    /**
     * Count active users
     */
    long countByStatus(UserStatus status);

    /**
     * Find locked users
     */
    List<User> findByStatusOrderByLockedDateDesc(UserStatus status);

    /**
     * Find users who logged in after date
     */
    List<User> findByLastLoginDateAfterOrderByLastLoginDateDesc(LocalDateTime date);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find all admins (ADMIN or SUPER_ADMIN)
     */
    @Query("SELECT u FROM User u WHERE u.rolesList LIKE '%ADMIN%'")
    List<User> findAllAdmins();

    /**
     * Find active users by department
     */
    Page<User> findByDepartmentAndStatus(String department, UserStatus status, Pageable pageable);
}
