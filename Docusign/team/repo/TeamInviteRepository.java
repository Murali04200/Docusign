package com.example.Docusign.team.repo;

import com.example.Docusign.team.model.TeamInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("teamInviteRepositoryAlt")
public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {
}
