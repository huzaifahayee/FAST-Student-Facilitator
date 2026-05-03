package com.fast.fsf.events.event;

import org.springframework.context.ApplicationEvent;

public class EventDeletedEvent extends ApplicationEvent {
    private final Long eventId;

    public EventDeletedEvent(Object source, Long eventId) {
        super(source);
        this.eventId = eventId;
    }

    public Long getEventId() {
        return eventId;
    }
}
