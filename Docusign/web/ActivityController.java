package com.example.Docusign.web;

import com.example.Docusign.model.Activity;
import com.example.Docusign.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/{memberId}/activities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Activity>> getMemberActivities(
            @PathVariable String memberId,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Activity> activities = activityService.getMemberActivities(memberId);
        return ResponseEntity.ok(activities);
    }
}
