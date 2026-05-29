package com.pfe.predictive.data.repository;

import com.pfe.predictive.core.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByMachineId(Long machineId);
    
    List<Task> findByAssignedTo(String assignedTo);
    
    List<Task> findByStatus(String status);
    
    List<Task> findByPriority(String priority);
    
    List<Task> findByDueDateBefore(LocalDateTime date);
    
    List<Task> findByDueDateBetween(LocalDateTime start, LocalDateTime end);
    
    List<Task> findByAssignedToAndStatus(String assignedTo, String status);
}
