package com.pfe.predictive.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "machine_technician_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"machine_id", "technician_id"}))
public class MachineTechnicianAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    public MachineTechnicianAssignment() {}

    public MachineTechnicianAssignment(Machine machine, User technician, User assignedBy) {
        this.machine = machine;
        this.technician = technician;
        this.assignedBy = assignedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public User getTechnician() {
        return technician;
    }

    public void setTechnician(User technician) {
        this.technician = technician;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
