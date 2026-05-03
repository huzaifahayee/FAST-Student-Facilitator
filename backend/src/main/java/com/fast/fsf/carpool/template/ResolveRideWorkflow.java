package com.fast.fsf.carpool.template;

import com.fast.fsf.carpool.event.RideResolvedEvent;
import com.fast.fsf.carpool.state.RideModerationContext;
import com.fast.fsf.carpool.domain.Ride;
import com.fast.fsf.carpool.persistence.RideRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ResolveRideWorkflow extends AbstractRideMutationWorkflow {

    public ResolveRideWorkflow(RideRepository rideRepository, ApplicationEventPublisher eventPublisher) {
        super(rideRepository, eventPublisher);
    }

    public ResponseEntity<Ride> resolve(Long id) {
        return execute(id, Optional.empty());
    }

    @Override
    protected Ride mutateRide(Ride ride, Optional<String> optionalReason) {
        RideModerationContext ctx = new RideModerationContext(ride);
        ctx.resolve();
        return ctx.getRide();
    }

    @Override
    protected void publishDomainEvent(Ride savedRide, Optional<String> optionalReason) {
        eventPublisher.publishEvent(new RideResolvedEvent(this, savedRide.getId()));
    }
}
