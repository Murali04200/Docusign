package com.example.Docusign.team.activity;

import com.example.Docusign.team.model.TeamMember;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamActivityService {

    private final TeamActivityRepository teamActivityRepository;

    public TeamActivityService(TeamActivityRepository teamActivityRepository) {
        this.teamActivityRepository = teamActivityRepository;
    }

    public void recordMembershipChange(Long teamId,
                                       TeamMember actor,
                                       String actionType,
                                       String detail) {
        if (teamId == null) {
            return;
        }
        TeamActivity activity = new TeamActivity();
        activity.setTeamId(teamId);
        if (actor != null) {
            activity.setActorUserId(actor.getUserId());
            activity.setActorDisplayName(actor.getDisplayName());
            activity.setActorEmail(actor.getEmail());
        }
        activity.setActionType(actionType != null ? actionType : "MEMBERSHIP");
        activity.setDetail(detail);
        teamActivityRepository.save(activity);
    }

    public void recordGeneric(Long teamId,
                              Long actorUserId,
                              String actorDisplayName,
                              String actorEmail,
                              String actionType,
                              String detail) {
        if (teamId == null) {
            return;
        }
        TeamActivity activity = new TeamActivity();
        activity.setTeamId(teamId);
        activity.setActorUserId(actorUserId);
        activity.setActorDisplayName(actorDisplayName);
        activity.setActorEmail(actorEmail);
        activity.setActionType(actionType);
        activity.setDetail(detail);
        teamActivityRepository.save(activity);
    }

    public List<TeamActivity> getRecentActivity(Long teamId, int limit) {
        if (teamId == null) {
            return List.of();
        }
        int pageSize = Math.max(1, Math.min(limit, 25));
        return teamActivityRepository.findByTeamIdOrderByCreatedAtDesc(teamId, PageRequest.of(0, pageSize));
    }
}
