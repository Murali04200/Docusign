package com.example.Docusign.team.activity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamActivityRepository extends JpaRepository<TeamActivity, Long> {
    List<TeamActivity> findByTeamIdOrderByCreatedAtDesc(Long teamId, Pageable pageable);
    
    // Find activities by actor user ID
    List<TeamActivity> findByActorUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
