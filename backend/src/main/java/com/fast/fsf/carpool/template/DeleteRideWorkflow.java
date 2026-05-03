package com.fast.fsf.carpool.template;

import com.fast.fsf.carpool.event.RideDeletedEvent;
import com.fast.fsf.carpool.persistence.RideRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Template Method pattern (GoF): encapsulates UC‑08 style permanent removal with the same sequencing as legacy code
 * (emit audit notification, then delete). We keep this class separate from {@link AbstractRideMutationWorkflow}
 * because the return type is {@code Void} rather than {@link com.fast.fsf.carpool.domain.Ride}.
 */
@Component
public class DeleteRideWorkflow {

    private final RideRepository rideRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DeleteRideWorkflow(RideRepository rideRepository, ApplicationEventPublisher eventPublisher) {
        this.rideRepository = rideRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Template method — final to preserve ordering guarantees for observers/transactions. */
    public final ResponseEntity<Void> execute(Long id, Optional<String> optionalReason) {
        return rideRepository.findById(id)
                .map(ride -> {
                    emitDeletionEvent(id, optionalReason.orElse(null));
                    rideRepository.deleteById(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Hook point (could be overridden in tests) separating “what to broadcast” from “how to delete”.
     */
    protected void emitDeletionEvent(Long id, String reasonOrNull) {
        eventPublisher.publishEvent(new RideDeletedEvent(this, id, reasonOrNull));
    }
}
