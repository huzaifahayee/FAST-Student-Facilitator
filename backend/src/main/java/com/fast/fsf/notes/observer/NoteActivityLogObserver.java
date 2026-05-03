package com.fast.fsf.notes.observer;

import com.fast.fsf.notes.event.NoteDeletedEvent;
import com.fast.fsf.notes.event.NoteUploadedEvent;
import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer pattern (GoF): this component is an <strong>Observer</strong> / subscriber reacting
 * to FAST-Notes lifecycle
 * {@linkplain org.springframework.context.ApplicationEvent ApplicationEvents}.
 * <p>
 * Spring's {@link EventListener} dispatches synchronously on the publishing thread by default,
 * so the audit row is inserted immediately after the transactional save — matching legacy behaviour.
 * <p>
 * Unlike the Lost &amp; Found feature (which uses its own Adapter), this observer writes directly
 * to the shared {@link ActivityLogRepository} to keep the Notes feature lean while still
 * satisfying the Observer decoupling intent: {@link com.fast.fsf.notes.service.FastNoteService}
 * never imports {@code ActivityLogRepository}.
 * <p>
 * Mirrors {@code RideActivityLogObserver} from the carpool feature.
 */
@Component
public class NoteActivityLogObserver {

    private final ActivityLogRepository activityLogRepository;

    public NoteActivityLogObserver(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    /**
     * Reacts when a new note PDF/DOCX is uploaded (UC "Upload FAST-Note").
     */
    @EventListener
    public void onNoteUploaded(NoteUploadedEvent event) {
        var note = event.getSavedNote();
        String msg = note.getStudentEmail()
                + " uploaded note \"" + note.getTitle()
                + "\" for " + note.getCourseCode() + " (" + note.getSubjectName() + ").";
        activityLogRepository.save(new ActivityLog(msg, "NOTE_UPLOADED"));
    }

    /**
     * Reacts when a note is soft-deleted by an admin (UC "Remove Note").
     */
    @EventListener
    public void onNoteDeleted(NoteDeletedEvent event) {
        String msg = "Note #" + event.getNoteId()
                + " removed by " + event.getDeletedByEmail() + ".";
        activityLogRepository.save(new ActivityLog(msg, "NOTE_REMOVED"));
    }
}
