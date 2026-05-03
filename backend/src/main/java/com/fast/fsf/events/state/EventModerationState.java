package com.fast.fsf.events.state;

import com.fast.fsf.events.domain.CampusEvent;

/**
 * State pattern (GoF): handles lifecycle transitions for a CampusEvent.
 */
public interface EventModerationState {
    void handle(CampusEvent event);
    String getStatusName();
}
