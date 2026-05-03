package com.fast.fsf.notes.event;

import org.springframework.context.ApplicationEvent;

/**
 * Observer pattern (GoF): domain event published by {@link com.fast.fsf.notes.service.FastNoteService}
 * after a {@link com.fast.fsf.notes.domain.FastNote} is soft-deleted (status set to "Removed")
 * by an admin (UC "Remove Note").
 */
public class NoteDeletedEvent extends ApplicationEvent {

    private final Long noteId;
    private final String deletedByEmail;

    public NoteDeletedEvent(Object source, Long noteId, String deletedByEmail) {
        super(source);
        this.noteId = noteId;
        this.deletedByEmail = deletedByEmail;
    }

    public Long getNoteId() {
        return noteId;
    }

    public String getDeletedByEmail() {
        return deletedByEmail;
    }
}
