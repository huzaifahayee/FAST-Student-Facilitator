package com.fast.fsf.search.catalog;

import com.fast.fsf.carpool.domain.Ride;
import com.fast.fsf.carpool.persistence.RideRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter pattern — concrete <strong>Adapter</strong> wrapping {@link RideRepository} (Adaptee) behind {@link ApprovedRideSource}.
 * <p>
 * Spring singleton bean — one adapter instance wires the catalog port for the whole application.
 */
@Component
public class RideRepositoryApprovedRideAdapter implements ApprovedRideSource {

    private final RideRepository rideRepository;

    public RideRepositoryApprovedRideAdapter(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    @Override
    public List<Ride> loadApprovedCandidates() {
        return rideRepository.findByApprovedTrue();
    }
}
