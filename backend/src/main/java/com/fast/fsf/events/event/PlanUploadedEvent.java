package com.fast.fsf.events.event;

import org.springframework.context.ApplicationEvent;

public class PlanUploadedEvent extends ApplicationEvent {
    private final int itemCount;

    public PlanUploadedEvent(Object source, int itemCount) {
        super(source);
        this.itemCount = itemCount;
    }

    public int getItemCount() {
        return itemCount;
    }
}
