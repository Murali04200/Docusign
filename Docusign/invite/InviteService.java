package com.example.Docusign.invite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class InviteService {
    private static final Logger log = LoggerFactory.getLogger(InviteService.class);

    private final InviteTokenRepository repo;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String mailFrom;

    public InviteService(InviteTokenRepository repo, JavaMailSender mailSender) {
        this.repo = repo;
        this.mailSender = mailSender;
    }

    public void sendAcceptanceEmailToInviter(InviteToken token) {
        if (token.getInviterEmail() == null || token.getInviterEmail().isBlank()) return;
        String subject = "Your team invite was accepted";
        String body = "Hi " + (token.getInviterName() != null ? token.getInviterName() : "there") + ",\n\n" +
                (token.getFullName() != null ? token.getFullName() : token.getEmail()) +
                " has accepted your invite" +
                (token.getAccountId() != null ? (" to account " + token.getAccountId()) : "") +
                (token.getAcceptedAt() != null ? (" at " + token.getAcceptedAt()) : ".") + "\n\n" +
                "Regards";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) message.setFrom(mailFrom);
            message.setTo(token.getInviterEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Acceptance email sent to inviter {}", token.getInviterEmail());
        } catch (Exception ex) {
            log.warn("Failed to notify inviter: {}", ex.toString());
        }
    }

    public InviteToken createInvite(String fullName, String email, String role, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        InviteToken token = InviteToken.create(fullName, email, role, expiresAt);
        return repo.save(token);
    }

    public InviteToken save(InviteToken token) {
        return repo.save(token);
    }

    public Optional<InviteToken> findByToken(String token) {
        return repo.findByToken(token);
    }

    public boolean isValid(InviteToken t) {
        return t != null && !t.isUsed() && t.getExpiresAt().isAfter(Instant.now());
    }

    public void markUsed(InviteToken t) {
        t.setUsed(true);
        repo.save(t);
    }

    public void sendInviteEmail(String baseUrl, InviteToken token) {
        String link = baseUrl + "/invite/accept?token=" + token.getToken();
        String subject = "You're invited to join the team";
        String body = "Hello " + token.getFullName() + ",\n\n" +
                "You've been invited to join the team with role: " + token.getRole() + ".\n" +
                "Click the secure one-time link below to accept your invite (expires at " + token.getExpiresAt() + "):\n\n" +
                link + "\n\n" +
                "If you did not expect this, you can ignore this email.";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom);
            }
            message.setTo(token.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Invite email sent to {} ({})", token.getFullName(), token.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to send email, falling back to logging the link. Cause: {}", ex.toString());
            log.info("Invite link for {} ({}): {}", token.getFullName(), token.getEmail(), link);
        }
    }
}
