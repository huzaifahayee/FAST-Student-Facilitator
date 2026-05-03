package com.fast.fsf.lostfound.persistence;

import com.fast.fsf.lostfound.domain.LostFoundListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LostFoundRepository extends JpaRepository<LostFoundListing, Long> {
    List<LostFoundListing> findByTypeOrderByDateDesc(String type);
    
    @Query("SELECT l FROM LostFoundListing l WHERE l.type = :type AND LOWER(l.itemName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<LostFoundListing> searchByTypeAndKeyword(@Param("type") String type, @Param("keyword") String keyword);
    
    @Query("SELECT l FROM LostFoundListing l WHERE l.type = :type AND l.category = :category AND LOWER(l.itemName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<LostFoundListing> searchByTypeAndCategoryAndKeyword(@Param("type") String type, @Param("category") String category, @Param("keyword") String keyword);

    List<LostFoundListing> findByTypeAndCategoryOrderByDateDesc(String type, String category);
}
