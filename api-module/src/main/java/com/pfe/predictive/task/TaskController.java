package com.pfe.predictive.task;

import com.pfe.predictive.core.entity.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Task Controller - Manage maintenance tasks
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management for technicians")
public class TaskController {
    
    private final TaskService taskService;
    
    /**
     * Get all tasks
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all tasks")
    public ResponseEntity<List<Task>> getAllTasks() {
        log.info("Fetching all tasks");
        List<Task> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * Get task by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        log.info("Fetching task: {}", id);
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }
    
    /**
     * Create new task
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create new task")
    public ResponseEntity<Task> createTask(@Valid @RequestBody TaskRequest request) {
        log.info("Creating task: {}", request.getTitle());
        Task saved = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    /**
     * Update task
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update task")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        
        log.info("Updating task: {}", id);
        Task updated = taskService.updateTask(id, request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Delete task
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete task")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        log.info("Deleting task: {}", id);
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get tasks by machine
     */
    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get tasks by machine")
    public ResponseEntity<List<Task>> getTasksByMachine(@PathVariable Long machineId) {
        log.info("Fetching tasks for machine: {}", machineId);
        List<Task> tasks = taskService.getTasksByMachine(machineId);
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * Get tasks by assigned user
     */
    @GetMapping("/assigned/{username}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get tasks by assigned user")
    public ResponseEntity<List<Task>> getTasksByAssignedUser(@PathVariable String username) {
        log.info("Fetching tasks for user: {}", username);
        List<Task> tasks = taskService.getTasksByAssignedTo(username);
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * Get tasks by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get tasks by status")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable String status) {
        log.info("Fetching tasks with status: {}", status);
        List<Task> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }
}
