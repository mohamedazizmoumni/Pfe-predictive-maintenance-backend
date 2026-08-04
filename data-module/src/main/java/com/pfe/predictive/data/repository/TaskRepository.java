package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByMachineId(Long machineId);

    // Paginated variant - completed/cancelled tasks accumulate indefinitely.
    Page<Task> findByMachineId(Long machineId, Pageable pageable);

    List<Task> findByAssignedTo(String assignedTo);

    Page<Task> findByAssignedTo(String assignedTo, Pageable pageable);

    List<Task> findByStatus(String status);

    Page<Task> findByStatus(String status, Pageable pageable);

    List<Task> findByPriority(String priority);

    List<Task> findByDueDateBefore(LocalDateTime date);

    List<Task> findByDueDateBetween(LocalDateTime start, LocalDateTime end);

    List<Task> findByAssignedToAndStatus(String assignedTo, String status);
}
