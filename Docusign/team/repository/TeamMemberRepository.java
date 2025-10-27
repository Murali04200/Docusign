package com.example.Docusign.team.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Docusign.team.model.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeamId(Long teamId);
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);
    long countByTeamId(Long teamId);
    List<TeamMember> findByUserId(Long userId);
    List<TeamMember> findByUserIdAndStatus(Long userId, String status);
}
