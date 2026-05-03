package com.fast.fsf.carpool.template;

import com.fast.fsf.carpool.event.RideApprovedEvent;
import com.fast.fsf.carpool.state.RideModerationContext;
import com.fast.fsf.carpool.domain.Ride;
import com.fast.fsf.carpool.persistence.RideRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ApproveRideWorkflow extends AbstractRideMutationWorkflow {

    public ApproveRideWorkflow(RideRepository rideRepository, ApplicationEventPublisher eventPublisher) {
        super(rideRepository, eventPublisher);
    }

    /** UC‑style approval endpoint delegates here — keeps controller thin. */
    public ResponseEntity<Ride> approve(Long id, Optional<String> optionalReason) {
        return execute(id, optionalReason);
    }

    @Override
    protected Ride mutateRide(Ride ride, Optional<String> optionalReason) {
        RideModerationContext ctx = new RideModerationContext(ride);
        ctx.approve(optionalReason.orElse(null));
        return ctx.getRide();
    }

    @Override
    protected void publishDomainEvent(Ride savedRide, Optional<String> optionalReason) {
        eventPublisher.publishEvent(new RideApprovedEvent(this, savedRide.getId(), optionalReason.orElse(null)));
    }
}
