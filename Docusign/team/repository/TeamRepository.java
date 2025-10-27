package com.example.Docusign.team.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Docusign.team.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByTeamCode(String teamCode);
    Optional<Team> findFirstByOwner_Id(Long ownerId);
    List<Team> findAllByOwner_Id(Long ownerId);
}
