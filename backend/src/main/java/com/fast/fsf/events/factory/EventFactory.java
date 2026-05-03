package com.fast.fsf.events.factory;

import com.fast.fsf.events.domain.CampusEvent;
import org.springframework.stereotype.Component;

/**
 * Factory Method pattern (GoF): centralises construction and validation rules for campus events (UC-21).
 */
@Component
public class EventFactory {

    /**
     * Normalises an incoming event before persistence.
     */
    public CampusEvent createEvent(CampusEvent incoming) {
        incoming.setId(null);
        // By default, if it's not explicitly approved, it stays as is (could be false).
        return incoming;
    }

    /**
     * Creates an entry for the semester plan.
     */
    public CampusEvent createPlanEntry(String title, String desc, java.time.LocalDate date,
                                       String venue, String organizer, String category, String ownerEmail) {
        CampusEvent ev = new CampusEvent();
        ev.setTitle(title);
        ev.setDescription(desc);
        ev.setEventDate(date);
        ev.setVenue(venue);
        ev.setOrganizer(organizer);
        ev.setCategory(category);
        ev.setSemesterPlan(true);
        ev.setOwnerEmail(ownerEmail);
        ev.setApproved(true);
        return ev;
    }
}
