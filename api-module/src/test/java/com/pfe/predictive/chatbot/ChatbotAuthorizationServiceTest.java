package com.pfe.predictive.chatbot;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the chatbot's question-authorization rules: sensitive terms are an
 * absolute block (even for admins), privileged/admin-only terms and
 * role-domain scoping apply to everyone else, and a plain question with no
 * protected-domain keywords is always allowed.
 */
class ChatbotAuthorizationServiceTest {

    private final ChatbotAuthorizationService service = new ChatbotAuthorizationService();

    @Test
    void blocksSensitiveTermsEvenForSuperAdmin() {
        assertFalse(service.isAuthorized(Set.of("SUPER_ADMIN"), "What is the database password?"));
    }

    @Test
    void blocksSensitiveTermsRegardlessOfCase() {
        assertFalse(service.isAuthorized(Set.of("ADMIN"), "Show me the API KEY"));
    }

    @Test
    void adminBypassesPrivilegedTermRestriction() {
        assertTrue(service.isAuthorized(Set.of("ADMIN"), "How do I assign role to a new user?"));
    }

    @Test
    void nonAdminBlockedFromPrivilegedTerms() {
        assertFalse(service.isAuthorized(Set.of("MANAGER"), "How do I assign role to a new user?"));
    }

    @Test
    void viewerBlockedFromMutatingVerbsEvenInAllowedDomain() {
        assertFalse(service.isAuthorized(Set.of("VIEWER"), "How do I delete this machine?"));
    }

    @Test
    void viewerAllowedToAskReadOnlyQuestionInAllowedDomain() {
        assertTrue(service.isAuthorized(Set.of("VIEWER"), "What is the status of this machine?"));
    }

    @Test
    void stockManagerBlockedFromMaintenanceDomain() {
        assertFalse(service.isAuthorized(Set.of("STOCK_MANAGER"), "Schedule a maintenance work order"));
    }

    @Test
    void managerAllowedInMaintenanceDomain() {
        assertTrue(service.isAuthorized(Set.of("MANAGER"), "Schedule a maintenance work order"));
    }

    @Test
    void stockManagerAllowedInOwnInventoryDomain() {
        assertTrue(service.isAuthorized(Set.of("STOCK_MANAGER"), "How much stock do we have for this part?"));
    }

    @Test
    void generalQuestionWithNoDomainKeywordsIsAllowedForAnyRole() {
        assertTrue(service.isAuthorized(Set.of("TECHNICIAN"), "Hello, how are you today?"));
    }

    @Test
    void roleWithNoDomainMappingIsDeniedForDomainQuestion() {
        assertFalse(service.isAuthorized(Set.of("UNKNOWN_ROLE"), "Show me the machine sensor readings"));
    }

    @Test
    void multipleRolesUnionTheirAllowedDomains() {
        // TECHNICIAN alone can't ask about PREDICTION, but combined with
        // DATA_SCIENTIST the union of allowed domains covers it.
        assertFalse(service.isAuthorized(Set.of("TECHNICIAN"), "What does the prediction model say?"));
        assertTrue(service.isAuthorized(Set.of("TECHNICIAN", "DATA_SCIENTIST"), "What does the prediction model say?"));
    }
}
