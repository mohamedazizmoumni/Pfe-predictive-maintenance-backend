package com.pfe.predictive.portal.service;

import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.data.repository.portal.CustomerMachineLinkRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Row-level authorization for the customer portal. Every portal endpoint
 * that touches a specific machine must call requireLinkedMachine() — this is
 * the one place "authenticated" and "authorized to see this machine"
 * actually diverge in the platform (see CustomerMachineLink).
 */
@Service
@RequiredArgsConstructor
public class PortalAccessService {

    private final UserRepository userRepository;
    private final CustomerMachineLinkRepository linkRepository;

    public User currentCustomer(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + authentication.getName()));
    }

    public void requireLinkedMachine(Long userId, Long machineId) {
        if (!linkRepository.existsByUserIdAndMachineId(userId, machineId)) {
            throw new AccessDeniedException("Machine " + machineId + " is not linked to this customer account");
        }
    }
}
