package com.pfe.predictive.data.repository.portal;

import com.pfe.predictive.core.entity.portal.CustomerMachineLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerMachineLinkRepository extends JpaRepository<CustomerMachineLink, Long> {

    List<CustomerMachineLink> findByUserId(Long userId);

    Optional<CustomerMachineLink> findByUserIdAndMachineId(Long userId, Long machineId);

    boolean existsByUserIdAndMachineId(Long userId, Long machineId);

    List<CustomerMachineLink> findByMachineId(Long machineId);
}
