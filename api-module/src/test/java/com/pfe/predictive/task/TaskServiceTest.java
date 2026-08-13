package com.pfe.predictive.task;

import com.pfe.predictive.common.service.AssignmentEmailContent;
import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.core.entity.Task;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.TaskRepository;
import com.pfe.predictive.data.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers TaskService's request-validation rules, status/priority defaults,
 * and the conditions under which an assignment email is (or is not) sent —
 * a bad assignedTo value or missing technician must never blow up task
 * creation/update, only skip the email.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, emailService, userRepository, machineRepository, maintenanceRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // createTask: reference validation + defaults
    // ------------------------------------------------------------------

    @Test
    void createTaskRejectsUnknownMachine() {
        when(machineRepository.existsById(42L)).thenReturn(false);

        TaskRequest request = TaskRequest.builder().title("Fix pump").machineId(42L).dueDate(LocalDateTime.now()).build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createTask(request));
        assertEquals("Machine not found with id 42", ex.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTaskRejectsUnknownMaintenance() {
        when(maintenanceRepository.existsById(7L)).thenReturn(false);

        TaskRequest request = TaskRequest.builder().title("Fix pump").maintenanceId(7L).dueDate(LocalDateTime.now()).build();

        assertThrows(IllegalArgumentException.class, () -> service.createTask(request));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTaskDefaultsPriorityAndStatusWhenOmitted() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TaskRequest request = TaskRequest.builder().title("Fix pump").dueDate(LocalDateTime.now()).build();
        Task saved = service.createTask(request);

        assertEquals("MEDIUM", saved.getPriority());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    void createTaskSkipsEmailWhenNoTechnicianAssigned() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TaskRequest request = TaskRequest.builder().title("Fix pump").dueDate(LocalDateTime.now()).build();
        service.createTask(request);

        verify(emailService, never()).sendAssignmentNotification(anyString(), any(AssignmentEmailContent.class));
    }

    @Test
    void createTaskSkipsEmailWhenAssignedToIsNotNumeric() {
        // Defensive case: assignedTo is always numeric when set via
        // assignedTechnicianId, but the email path re-parses it and must
        // not throw if that ever drifts.
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            t.setAssignedTo("not-a-number");
            return t;
        });

        TaskRequest request = TaskRequest.builder().title("Fix pump").assignedTechnicianId(5L).dueDate(LocalDateTime.now()).build();
        service.createTask(request);

        verify(emailService, never()).sendAssignmentNotification(anyString(), any(AssignmentEmailContent.class));
    }

    @Test
    void createTaskSkipsEmailWhenTechnicianNotFound() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        TaskRequest request = TaskRequest.builder().title("Fix pump").assignedTechnicianId(5L).dueDate(LocalDateTime.now()).build();
        service.createTask(request);

        verify(emailService, never()).sendAssignmentNotification(anyString(), any(AssignmentEmailContent.class));
    }

    @Test
    void createTaskSkipsEmailWhenTechnicianHasNoEmail() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        User technician = new User();
        technician.setUsername("jane.tech");
        technician.setEmail("");
        when(userRepository.findById(5L)).thenReturn(Optional.of(technician));

        TaskRequest request = TaskRequest.builder().title("Fix pump").assignedTechnicianId(5L).dueDate(LocalDateTime.now()).build();
        service.createTask(request);

        verify(emailService, never()).sendAssignmentNotification(anyString(), any(AssignmentEmailContent.class));
    }

    @Test
    void createTaskSendsEmailWhenTechnicianResolves() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        User technician = new User();
        technician.setUsername("jane.tech");
        technician.setEmail("jane@sentinel.io");
        technician.setFirstName("Jane");
        when(userRepository.findById(5L)).thenReturn(Optional.of(technician));

        TaskRequest request = TaskRequest.builder().title("Fix pump").assignedTechnicianId(5L).dueDate(LocalDateTime.now()).build();
        service.createTask(request);

        verify(emailService, times(1)).sendAssignmentNotification(eq("jane@sentinel.io"), any(AssignmentEmailContent.class));
    }

    // ------------------------------------------------------------------
    // updateTask: partial updates + re-notify only on reassignment
    // ------------------------------------------------------------------

    @Test
    void updateTaskOnlyTouchesProvidedFields() {
        Task existing = Task.builder().id(1L).title("Original").description("Original desc").priority("LOW").status("PENDING").build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest request = TaskRequest.builder().priority("HIGH").build();
        Task updated = service.updateTask(1L, request);

        assertEquals("Original", updated.getTitle());
        assertEquals("Original desc", updated.getDescription());
        assertEquals("HIGH", updated.getPriority());
    }

    @Test
    void updateTaskDoesNotResendEmailWhenAssignmentUnchanged() {
        Task existing = Task.builder().id(1L).title("Original").assignedTo("5").build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest request = TaskRequest.builder().assignedTechnicianId(5L).build();
        service.updateTask(1L, request);

        verify(emailService, never()).sendAssignmentNotification(anyString(), any(AssignmentEmailContent.class));
    }

    @Test
    void updateTaskResendsEmailWhenReassignedToSomeoneElse() {
        Task existing = Task.builder().id(1L).title("Original").assignedTo("5").build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        User newTechnician = new User();
        newTechnician.setUsername("bob.tech");
        newTechnician.setEmail("bob@sentinel.io");
        when(userRepository.findById(9L)).thenReturn(Optional.of(newTechnician));

        TaskRequest request = TaskRequest.builder().assignedTechnicianId(9L).build();
        service.updateTask(1L, request);

        verify(emailService, times(1)).sendAssignmentNotification(eq("bob@sentinel.io"), any(AssignmentEmailContent.class));
    }

    @Test
    void updateTaskThrowsWhenTaskMissing() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.updateTask(404L, TaskRequest.builder().build()));
    }

    // ------------------------------------------------------------------
    // deleteTask
    // ------------------------------------------------------------------

    @Test
    void deleteTaskThrowsWhenMissing() {
        when(taskRepository.existsById(404L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.deleteTask(404L));
        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    void deleteTaskRemovesExistingTask() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        service.deleteTask(1L);

        verify(taskRepository).deleteById(1L);
    }
}
