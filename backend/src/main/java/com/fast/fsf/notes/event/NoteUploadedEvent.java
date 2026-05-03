package com.fast.fsf.notes.event;

import com.fast.fsf.notes.domain.FastNote;
import org.springframework.context.ApplicationEvent;

/**
 * Observer pattern (GoF): domain event published by {@link com.fast.fsf.notes.service.FastNoteService}
 * after a new {@link FastNote} PDF/DOCX is saved to disk and its record persisted
 * (UC "Upload FAST-Note").
 * <p>
 * Observer subscribers ({@link com.fast.fsf.notes.observer.NoteActivityLogObserver}) react
 * without the service touching {@code ActivityLogRepository} directly.
 */
public class NoteUploadedEvent extends ApplicationEvent {

    private final FastNote savedNote;

    public NoteUploadedEvent(Object source, FastNote savedNote) {
        super(source);
        this.savedNote = savedNote;
    }

    public FastNote getSavedNote() {
        return savedNote;
    }
}
