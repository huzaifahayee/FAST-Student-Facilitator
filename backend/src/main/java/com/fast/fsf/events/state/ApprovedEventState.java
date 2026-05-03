package com.fast.fsf.events.state;

import com.fast.fsf.events.domain.CampusEvent;

public class ApprovedEventState implements EventModerationState {
    @Override
    public void handle(CampusEvent event) {
        event.setApproved(true);
    }

    @Override
    public String getStatusName() {
        return "APPROVED";
    }
}
