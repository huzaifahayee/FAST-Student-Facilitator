package com.fast.fsf.books.persistence;

import com.fast.fsf.books.domain.BookListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookListingRepository extends JpaRepository<BookListing, Long> {

    // Custom moderation queries
    long countByFlaggedTrue();
    List<BookListing> findByFlaggedTrue();
    
    // Approval filtering
    long countByApprovedTrue();
    List<BookListing> findByApprovedTrue();
    List<BookListing> findByApprovedFalse();
    long countByApprovedFalse();
    
    // Feature specific queries
    List<BookListing> findByListingTypeAndApprovedTrueOrderByStatusAsc(String listingType);
    List<BookListing> findByOwnerEmail(String ownerEmail);
    
    @Query("SELECT b FROM BookListing b WHERE b.approved = true AND b.listingType = :type AND (LOWER(b.bookTitle) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.courseCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<BookListing> searchApprovedListings(@Param("query") String query, @Param("type") String type);
}
