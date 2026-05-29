package com.pfe.predictive.maintenancecost.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when machine failure occurs
 * Triggers automatic budget update
 */
@Getter
public class FailureOccurredEvent extends ApplicationEvent {
    
    private final Long failureEventId;

    public FailureOccurredEvent(Object source, Long failureEventId) {
        super(source);
        this.failureEventId = failureEventId;
    }
}
