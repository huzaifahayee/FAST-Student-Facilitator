package com.fast.fsf.events.event;

import com.fast.fsf.events.domain.CampusEvent;
import org.springframework.context.ApplicationEvent;

public class EventApprovedEvent extends ApplicationEvent {
    private final CampusEvent event;

    public EventApprovedEvent(Object source, CampusEvent event) {
        super(source);
        this.event = event;
    }

    public CampusEvent getEvent() {
        return event;
    }
}
