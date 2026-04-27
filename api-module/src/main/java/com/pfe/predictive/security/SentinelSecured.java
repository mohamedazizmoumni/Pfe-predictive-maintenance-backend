package com.pfe.predictive.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Custom annotation for role-based access control with predefined role sets.
 * 
 * Provides common authorization patterns without repeating long @PreAuthorize expressions.
 * Examples:
 * - @SentinelSecured(SentinelRole.PUBLIC) - all authenticated users
 * - @SentinelSecured(SentinelRole.MANAGER_AND_ABOVE) - MANAGER, ADMIN, SUPER_ADMIN
 * - @SentinelSecured(SentinelRole.ADMIN_ONLY) - SUPER_ADMIN only
 * 
 * Wrapper around @PreAuthorize to reduce annotation noise.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SentinelSecured {
    /**
     * Predefined role set to grant access.
     */
    SentinelRole value() default SentinelRole.AUTHENTICATED;

    /**
     * Predefined role groupings for common authorization patterns.
     */
    enum SentinelRole {
        /**
         * Any authenticated user (all 6 roles).
         */
        AUTHENTICATED("hasAnyRole('TECHNICIAN','MANAGER','STOCK_MANAGER','DATA_SCIENTIST','ADMIN','SUPER_ADMIN')"),

        /**
         * Technicians and above (handles on-floor equipment).
         */
        TECHNICIAN_AND_ABOVE("hasAnyRole('TECHNICIAN','MANAGER','DATA_SCIENTIST','ADMIN','SUPER_ADMIN')"),

        /**
         * Managers, Admins (supervisory roles).
         */
        MANAGER_AND_ABOVE("hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN')"),

        /**
         * Admins only (system administrators).
         */
        ADMIN_AND_ABOVE("hasAnyRole('ADMIN','SUPER_ADMIN')"),

        /**
         * Super admin only (highest privilege).
         */
        SUPER_ADMIN_ONLY("hasRole('SUPER_ADMIN')"),

        /**
         * Data scientists and admins (for ML/analytics).
         */
        DATA_SCIENTIST_AND_ABOVE("hasAnyRole('DATA_SCIENTIST','ADMIN','SUPER_ADMIN')"),

        /**
         * Stock managers and admins (for inventory).
         */
        STOCK_MANAGER_AND_ABOVE("hasAnyRole('STOCK_MANAGER','ADMIN','SUPER_ADMIN')");

        public final String expression;

        SentinelRole(String expression) {
            this.expression = expression;
        }
    }
}
