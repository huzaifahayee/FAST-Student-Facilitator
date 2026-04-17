package com.fast.fsf.repository;

import com.fast.fsf.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * RideRepository
 * 
 * What is this?
 * This is an interface that allows Spring to handle all database operations 
 * for 'Rides' (Save, Delete, Find) automatically.
 * 
 * SOLID Note: 
 * By using an interface, we follow the Dependency Inversion Principle. 
 * Our application depends on this 'abstraction' rather than a specific
 * database implementation.
 */
@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    
    // Custom moderation queries
    long countByFlaggedTrue();
    List<Ride> findByFlaggedTrue();
    
    // Approval filtering
    long countByApprovedTrue();
    List<Ride> findByApprovedTrue();
    List<Ride> findByApprovedFalse();
    long countByApprovedFalse();
    
    List<Ride> findByDestinationContainingIgnoreCaseAndApprovedTrue(String destination);
    List<Ride> findByOriginContainingIgnoreCaseAndApprovedTrue(String origin);
}
