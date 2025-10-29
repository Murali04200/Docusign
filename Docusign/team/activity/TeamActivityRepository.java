package com.example.Docusign.team.activity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamActivityRepository extends JpaRepository<TeamActivity, Long> {
    List<TeamActivity> findByTeamIdOrderByCreatedAtDesc(Long teamId, Pageable pageable);
    
    // Find activities by actor user ID
    List<TeamActivity> findByActorUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // Find documents pending signature for a specific user
    @Query("SELECT a FROM TeamActivity a WHERE a.actionType = :actionType AND a.detail LIKE %:userEmail% ORDER BY a.createdAt DESC")
    List<TeamActivity> findByActionTypeAndDetailContaining(
        @Param("actionType") String actionType, 
        @Param("userEmail") String userEmail
    );
}
