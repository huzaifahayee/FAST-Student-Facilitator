package com.fast.fsf.events.state;

import com.fast.fsf.events.domain.CampusEvent;

public class PendingEventState implements EventModerationState {
    @Override
    public void handle(CampusEvent event) {
        event.setApproved(false);
    }

    @Override
    public String getStatusName() {
        return "PENDING";
    }
}
