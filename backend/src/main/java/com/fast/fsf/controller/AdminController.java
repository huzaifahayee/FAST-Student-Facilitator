package com.fast.fsf.controller;

import com.fast.fsf.model.ActivityLog;
import com.fast.fsf.repository.ActivityLogRepository;
import com.fast.fsf.repository.RideRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminController
 * 
 * Provides centralized statistics and logging for the Portal Admin.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final ActivityLogRepository activityLogRepository;
    private final RideRepository rideRepository;

    public AdminController(ActivityLogRepository activityLogRepository, 
                           RideRepository rideRepository) {
        this.activityLogRepository = activityLogRepository;
        this.rideRepository = rideRepository;
    }

    /**
     * GET /api/admin/logs
     * Returns the 10 most recent activity logs for the sidebar feed.
     */
    @GetMapping("/logs")
    public List<ActivityLog> getRecentLogs() {
        return activityLogRepository.findTop10ByOrderByIdDesc();
    }

    /**
     * GET /api/admin/pending/count
     * Returns the number of items waiting for approval.
     */
    @GetMapping("/pending/count")
    public long getPendingCount() {
        return rideRepository.countByApprovedFalse();
    }
}
