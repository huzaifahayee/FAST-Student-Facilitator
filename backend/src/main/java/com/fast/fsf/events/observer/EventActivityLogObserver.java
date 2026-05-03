package com.fast.fsf.events.observer;

import com.fast.fsf.events.adapter.EventActivityLogAdapter;
import com.fast.fsf.events.event.EventApprovedEvent;
import com.fast.fsf.events.event.EventCreatedEvent;
import com.fast.fsf.events.event.EventDeletedEvent;
import com.fast.fsf.events.event.EventEditedEvent;
import com.fast.fsf.events.event.PlanUploadedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer pattern (GoF): reacts to event domain events to record activity logs.
 */
@Component
public class EventActivityLogObserver {

    private final EventActivityLogAdapter activityLogAdapter;

    public EventActivityLogObserver(EventActivityLogAdapter activityLogAdapter) {
        this.activityLogAdapter = activityLogAdapter;
    }

    @EventListener
    public void onEventCreated(EventCreatedEvent event) {
        var e = event.getEvent();
        String msg = "Campus event added: " + e.getTitle() + (e.isApproved() ? " (Auto-approved)" : " (Pending)");
        String type = e.isApproved() ? "EVENT_ADDED" : "EVENT_PROPOSED";
        activityLogAdapter.record(msg, type);
    }

    @EventListener
    public void onEventApproved(EventApprovedEvent event) {
        var e = event.getEvent();
        activityLogAdapter.record("Campus event approved: " + e.getTitle(), "EVENT_APPROVED");
    }

    @EventListener
    public void onEventEdited(EventEditedEvent event) {
        var e = event.getEvent();
        activityLogAdapter.record("Campus event edited: " + e.getTitle(), "EVENT_EDITED");
    }

    @EventListener
    public void onEventDeleted(EventDeletedEvent event) {
        activityLogAdapter.record("Campus event deleted: #" + event.getEventId(), "EVENT_DELETED");
    }

    @EventListener
    public void onPlanUploaded(PlanUploadedEvent event) {
        activityLogAdapter.record("Semester Plan uploaded with " + event.getItemCount() + " items.", "PLAN_UPLOADED");
    }
}
