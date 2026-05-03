package com.fast.fsf.shared.persistence;

import com.fast.fsf.shared.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    // Fetch the 10 most recent logs ordered by ID descending (newest first)
    List<ActivityLog> findTop10ByOrderByIdDesc();
}
