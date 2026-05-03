package com.fast.fsf.carpool.template;

import com.fast.fsf.carpool.event.RideFlaggedEvent;
import com.fast.fsf.carpool.state.RideModerationContext;
import com.fast.fsf.carpool.domain.Ride;
import com.fast.fsf.carpool.persistence.RideRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FlagRideWorkflow extends AbstractRideMutationWorkflow {

    public FlagRideWorkflow(RideRepository rideRepository, ApplicationEventPublisher eventPublisher) {
        super(rideRepository, eventPublisher);
    }

    public ResponseEntity<Ride> flag(Long id, Optional<String> optionalReason) {
        return execute(id, optionalReason);
    }

    @Override
    protected Ride mutateRide(Ride ride, Optional<String> optionalReason) {
        RideModerationContext ctx = new RideModerationContext(ride);
        ctx.flag(optionalReason.orElse(null));
        return ctx.getRide();
    }

    @Override
    protected void publishDomainEvent(Ride savedRide, Optional<String> optionalReason) {
        eventPublisher.publishEvent(new RideFlaggedEvent(this, savedRide.getId(), optionalReason.orElse(null)));
    }
}
