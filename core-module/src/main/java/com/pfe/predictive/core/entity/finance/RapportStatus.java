package com.pfe.predictive.core.entity.finance;

/**
 * Lifecycle status of a maintenance rapport.
 * Flow: PENDING_MANAGER_APPROVAL -> PENDING_FINANCE_APPROVAL -> APPROVED, with REJECTED reachable from either approval step.
 */
public enum RapportStatus {
    PENDING_MANAGER_APPROVAL,
    PENDING_FINANCE_APPROVAL,
    APPROVED,
    REJECTED
}
