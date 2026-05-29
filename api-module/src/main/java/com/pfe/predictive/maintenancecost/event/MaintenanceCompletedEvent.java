package com.pfe.predictive.maintenancecost.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when maintenance action is completed
 * Triggers automatic budget update
 */
@Getter
public class MaintenanceCompletedEvent extends ApplicationEvent {
    
    private final Long maintenanceActionId;

    public MaintenanceCompletedEvent(Object source, Long maintenanceActionId) {
        super(source);
        this.maintenanceActionId = maintenanceActionId;
    }
}
