package com.example.Docusign.team.repo;

import com.example.Docusign.team.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("teamRepositoryAlt")
public interface TeamRepository extends JpaRepository<Team, Long> {
}
