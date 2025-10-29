package com.example.Docusign.service;

import com.example.Docusign.model.Activity;
import com.example.Docusign.team.activity.TeamActivity;
import com.example.Docusign.team.activity.TeamActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class ActivityService {

    private final TeamActivityRepository teamActivityRepository;

    public ActivityService(TeamActivityRepository teamActivityRepository) {
        this.teamActivityRepository = teamActivityRepository;
    }
    
    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    @Transactional(readOnly = true)
    public List<Activity> getMemberActivities(String memberId) {
        try {
            log.debug("Fetching activities for member: {}", memberId);
            
            // Convert memberId to Long
            Long userId;
            try {
                userId = Long.parseLong(memberId);
            } catch (NumberFormatException e) {
                log.warn("Invalid member ID format: {}", memberId);
                return List.of();
            }
            
            // Get the most recent 10 activities for the user
            Pageable pageable = PageRequest.of(0, 10);
            
            // Fetch activities from the database
            List<TeamActivity> teamActivities;
            try {
                teamActivities = teamActivityRepository.findByActorUserIdOrderByCreatedAtDesc(userId, pageable);
                log.debug("Found {} activities for user {}", teamActivities.size(), userId);
            } catch (Exception e) {
                log.error("Error fetching activities for user {}: {}", userId, e.getMessage(), e);
                return List.of();
            }
            
            // If no activities found, return an empty list
            if (teamActivities == null || teamActivities.isEmpty()) {
                log.debug("No activities found for user: {}", userId);
                return List.of();
            }
            
            // Convert TeamActivity to Activity DTO
            try {
                return teamActivities.stream()
                    .map(this::mapToActivity)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Error mapping activities for user {}: {}", userId, e.getMessage(), e);
                return List.of();
            }
                
        } catch (Exception e) {
            log.error("Unexpected error in getMemberActivities: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    private Activity mapToActivity(TeamActivity teamActivity) {
        if (teamActivity == null) {
            log.warn("Attempted to map null TeamActivity");
            return createEmptyActivity();
        }
        
        try {
            Activity activity = new Activity();
            activity.setId(teamActivity.getId() != null ? String.valueOf(teamActivity.getId()) : "unknown");
            activity.setMemberId(teamActivity.getActorUserId() != null ? 
                              String.valueOf(teamActivity.getActorUserId()) : "unknown");
            
            // Extract type and action from actionType
            String actionType = teamActivity.getActionType();
            String type = "document";
            String action = "updated";
            
            if (actionType != null && !actionType.isEmpty()) {
                String[] parts = actionType.split("_", 2);
                if (parts.length > 0) type = parts[0].toLowerCase();
                if (parts.length > 1) action = parts[1].toLowerCase();
            }
            
            activity.setType(type);
            activity.setAction(action);
            
            // Set title from detail or generate one
            String detail = teamActivity.getDetail();
            activity.setTitle(detail != null && !detail.isEmpty() ? 
                            detail.substring(0, Math.min(detail.length(), 100)) : 
                            String.format("%s %s", action, type));
            
            // Set timestamp
            if (teamActivity.getCreatedAt() != null) {
                activity.setTimestamp(LocalDateTime.ofInstant(teamActivity.getCreatedAt(), ZoneId.systemDefault()));
            } else {
                activity.setTimestamp(LocalDateTime.now());
            }
            
            // Set details
            String displayName = teamActivity.getActorDisplayName();
            activity.setDetails(detail != null ? 
                              detail : 
                              String.format("%s %s by %s", 
                                          action, 
                                          type, 
                                          displayName != null ? displayName : "a user"));
            
            return activity;
            
        } catch (Exception e) {
            log.error("Error mapping activity {}: {}", teamActivity.getId(), e.getMessage(), e);
            return createEmptyActivity();
        }
    }
    
    private Activity createEmptyActivity() {
        Activity activity = new Activity();
        activity.setId("error");
        activity.setMemberId("unknown");
        activity.setType("error");
        activity.setAction("error");
        activity.setTitle("Error loading activity");
        activity.setTimestamp(LocalDateTime.now());
        activity.setDetails("There was an error loading this activity");
        return activity;
    }
}
