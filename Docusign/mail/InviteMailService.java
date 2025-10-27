package com.example.Docusign.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class InviteMailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public InviteMailService(JavaMailSender mailSender,
                             @Value("${spring.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendTeamInvite(String toEmail,
                               String recipientName,
                               String role,
                               String teamName,
                               String inviteLink) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        if (fromAddress != null && !fromAddress.isBlank()) {
            msg.setFrom(fromAddress);
        }
        msg.setSubject("You're invited to join team" + (teamName != null && !teamName.isBlank() ? (" - " + teamName) : ""));

        StringBuilder body = new StringBuilder();
        body.append("Hello");
        if (recipientName != null && !recipientName.isBlank()) {
            body.append(" ").append(recipientName);
        }
        body.append(",\n");
        body.append("You've been invited to join the team");
        if (teamName != null && !teamName.isBlank()) {
            body.append(" ").append(teamName);
        }
        if (role != null && !role.isBlank()) {
            body.append(" as ").append(role);
        }
        body.append(".\n");
        body.append("Accept Invite\n");
        if (inviteLink != null && !inviteLink.isBlank()) {
            body.append("Or copy this link into your browser:\n");
            body.append(inviteLink).append("\n");
        }
        body.append("If you did not expect this email, you can ignore it.\n");
        body.append("Thanks,\nDocusign Team");

        msg.setText(body.toString());
        mailSender.send(msg);
    }

    public void sendTeamInviteHtml(String toEmail,
                                   String recipientName,
                                   String role,
                                   String teamName,
                                   String inviteLink) throws Exception {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setTo(toEmail);
        if (fromAddress != null && !fromAddress.isBlank()) {
            helper.setFrom(fromAddress);
        }
        helper.setSubject("You're invited to join team" + (teamName != null && !teamName.isBlank() ? (" - " + teamName) : ""));

        String safeName = recipientName != null ? recipientName : "";
        String safeTeam = teamName != null ? teamName : "";
        String safeRole = role != null ? role : "";
        String safeLink = inviteLink != null ? inviteLink : "#";

        StringBuilder html = new StringBuilder();
        html.append("<div style=\"font-family:Inter,Arial,sans-serif;line-height:1.6;color:#0f172a\">");
        html.append("<p style=\"margin:0 0 12px\">Hello");
        if (!safeName.isBlank()) html.append(" ").append(escape(safeName));
        html.append(",</p>");
        html.append("<p style=\"margin:0 0 16px\">You've been invited to join the team ")
            .append(escape(safeTeam));
        if (!safeRole.isBlank()) html.append(" as ").append(escape(safeRole));
        html.append(".</p>");
        html.append("<p style=\"margin:0 0 16px\"><a href=\"")
            .append(escape(safeLink))
            .append("\" style=\"display:inline-block;padding:12px 18px;background:#0d6efd;color:#fff;text-decoration:none;border-radius:8px\">Accept Invite</a></p>");
        html.append("<p style=\"margin:0 0 8px\">Or copy this link into your browser:</p>");
        html.append("<p style=\"margin:0 0 16px\"><a href=\"")
            .append(escape(safeLink)).append("\">")
            .append(escape(safeLink)).append("</a></p>");
        html.append("<p style=\"margin:0 0 6px\">If you did not expect this email, you can ignore it.</p>");
        html.append("<p style=\"margin:0\">Thanks,<br/>Docusign Team</p>");
        html.append("</div>");

        helper.setText(html.toString(), true);
        mailSender.send(mime);
    }

    // minimal HTML escape for &, <, > and quotes
    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Notify sender (e.g., team owner) that the invite was accepted
    public void sendInviteAccepted(String toEmail,
                                   String inviteeEmail,
                                   String teamName) throws Exception {
        // HTML version
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setTo(toEmail);
        if (fromAddress != null && !fromAddress.isBlank()) {
            helper.setFrom(fromAddress);
        }
        helper.setSubject("Invite accepted - " + (teamName != null ? teamName : "Team"));
        String safeInvitee = inviteeEmail != null ? inviteeEmail : "the recipient";
        String safeTeam = teamName != null ? teamName : "your team";
        String html = "<div style=\"font-family:Inter,Arial,sans-serif;line-height:1.6;color:#0f172a\">" +
                "<p style=\"margin:0 0 12px\">Good news!</p>" +
                "<p style=\"margin:0 0 16px\"><strong>" + escape(safeInvitee) + "</strong> has accepted the invitation to join <strong>" + escape(safeTeam) + "</strong>.</p>" +
                "<p style=\"margin:0\">Thanks,<br/>Docusign Team</p>" +
                "</div>";
        helper.setText(html, true);
        mailSender.send(mime);

        // Text fallback
        SimpleMailMessage txt = new SimpleMailMessage();
        txt.setTo(toEmail);
        if (fromAddress != null && !fromAddress.isBlank()) {
            txt.setFrom(fromAddress);
        }
        txt.setSubject("Invite accepted - " + (teamName != null ? teamName : "Team"));
        txt.setText("Good news!\n" + (inviteeEmail != null ? inviteeEmail : "The recipient") +
                " has accepted the invitation to join " + (teamName != null ? teamName : "your team") + ".\n\nThanks,\nDocusign Team");
        mailSender.send(txt);
    }
}
