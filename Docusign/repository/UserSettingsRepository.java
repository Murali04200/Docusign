package com.example.Docusign.repository;

import com.example.Docusign.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, String> {

    Optional<UserSettings> findByUserId(String userId);
}
