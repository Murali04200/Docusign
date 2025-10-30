package com.example.Docusign.mail;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EnvelopeMailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String baseUrl;

    public EnvelopeMailService(JavaMailSender mailSender,
                               @Value("${spring.mail.from:noreply@docusign.local}") String fromAddress,
                               @Value("${app.base-url:http://localhost:8082}") String baseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
    }

    /**
     * Send signature request to recipient
     */
    public void sendSignatureRequest(String toEmail,
                                     String recipientName,
                                     String senderName,
                                     String documentName,
                                     String subject,
                                     String message,
                                     String envelopeId) throws Exception {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

        helper.setTo(toEmail);
        if (fromAddress != null && !fromAddress.isBlank()) {
            helper.setFrom(fromAddress);
        }

        String emailSubject = subject != null && !subject.isBlank()
            ? subject
            : "Please sign: " + documentName;
        helper.setSubject(emailSubject);

        String signingLink = baseUrl + "/sign-document?envelopeId=" + envelopeId + "&email=" + toEmail;

        String safeName = recipientName != null ? recipientName : "";
        String safeSender = senderName != null ? senderName : "DocuSign";
        String safeDocument = documentName != null ? documentName : "Document";
        String safeMessage = message != null ? message : "You have been requested to sign a document.";
        String safeLink = signingLink;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>");
        html.append("<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif\">");
        html.append("<div style=\"max-width:600px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07)\">");

        // Header with DocuSign branding
        html.append("<div style=\"background:linear-gradient(135deg,#0d6efd 0%,#0a58ca 100%);padding:32px 24px;text-align:center\">");
        html.append("<div style=\"display:inline-block;background:rgba(255,255,255,0.15);padding:12px 24px;border-radius:8px;margin-bottom:12px\">");
        html.append("<svg width=\"28\" height=\"28\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#fff\" stroke-width=\"2.5\">");
        html.append("<path d=\"M9 12l2 2 4-4\"/><circle cx=\"12\" cy=\"12\" r=\"10\"/></svg>");
        html.append("</div>");
        html.append("<h1 style=\"margin:0;color:#fff;font-size:24px;font-weight:600\">DocuSign</h1>");
        html.append("<p style=\"margin:8px 0 0;color:rgba(255,255,255,0.9);font-size:14px\">Signature Request</p>");
        html.append("</div>");

        // Content
        html.append("<div style=\"padding:32px 24px\">");

        if (!safeName.isBlank()) {
            html.append("<p style=\"margin:0 0 16px;font-size:16px;color:#0f172a\">Hello <strong>");
            html.append(escape(safeName));
            html.append("</strong>,</p>");
        } else {
            html.append("<p style=\"margin:0 0 16px;font-size:16px;color:#0f172a\">Hello,</p>");
        }

        html.append("<p style=\"margin:0 0 16px;font-size:15px;color:#334155;line-height:1.6\">");
        html.append("<strong>").append(escape(safeSender)).append("</strong> has requested your signature on the following document:</p>");

        // Document info box
        html.append("<div style=\"background:#f8fafc;border-left:4px solid #0d6efd;padding:16px;margin:0 0 24px;border-radius:6px\">");
        html.append("<div style=\"display:flex;align-items:center;gap:12px\">");
        html.append("<svg width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#0d6efd\" stroke-width=\"2\">");
        html.append("<path d=\"M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z\"/><polyline points=\"14 2 14 8 20 8\"/></svg>");
        html.append("<div>");
        html.append("<p style=\"margin:0;font-size:14px;color:#64748b\">Document Name</p>");
        html.append("<p style=\"margin:4px 0 0;font-size:15px;color:#0f172a;font-weight:600\">");
        html.append(escape(safeDocument));
        html.append("</p></div></div>");

        if (!safeMessage.isBlank() && !safeMessage.equals("You have been requested to sign a document.")) {
            html.append("<div style=\"margin-top:16px;padding-top:16px;border-top:1px solid #e2e8f0\">");
            html.append("<p style=\"margin:0;font-size:14px;color:#64748b\">Message from sender:</p>");
            html.append("<p style=\"margin:8px 0 0;font-size:14px;color:#334155;line-height:1.5\">");
            html.append(escape(safeMessage));
            html.append("</p></div>");
        }
        html.append("</div>");

        // CTA Button
        html.append("<div style=\"text-align:center;margin:0 0 24px\">");
        html.append("<a href=\"").append(escape(safeLink));
        html.append("\" style=\"display:inline-block;padding:14px 32px;background:#0d6efd;color:#fff;text-decoration:none;border-radius:8px;font-weight:600;font-size:15px;box-shadow:0 2px 4px rgba(13,110,253,0.3)\">");
        html.append("Review and Sign Document");
        html.append("</a></div>");

        // Alternative link
        html.append("<p style=\"margin:0 0 8px;font-size:13px;color:#64748b;text-align:center\">Or copy this link:</p>");
        html.append("<p style=\"margin:0 0 24px;font-size:12px;word-break:break-all;text-align:center\">");
        html.append("<a href=\"").append(escape(safeLink)).append("\" style=\"color:#0d6efd;text-decoration:none\">");
        html.append(escape(safeLink));
        html.append("</a></p>");

        // Security note
        html.append("<div style=\"background:#fef3c7;border:1px solid #fbbf24;border-radius:6px;padding:12px;margin:0 0 24px\">");
        html.append("<p style=\"margin:0;font-size:13px;color:#92400e;line-height:1.5\">");
        html.append("🔒 <strong>Security Notice:</strong> This is a legitimate signature request from DocuSign. ");
        html.append("Always verify the sender before signing any document.");
        html.append("</p></div>");

        html.append("<p style=\"margin:0;font-size:13px;color:#64748b;line-height:1.6\">");
        html.append("If you did not expect this signature request, please contact the sender or disregard this email.");
        html.append("</p>");

        html.append("</div>");

        // Footer
        html.append("<div style=\"background:#f8fafc;padding:24px;text-align:center;border-top:1px solid #e2e8f0\">");
        html.append("<p style=\"margin:0 0 8px;font-size:14px;color:#0f172a;font-weight:600\">DocuSign</p>");
        html.append("<p style=\"margin:0;font-size:12px;color:#64748b\">The Digital Transaction Management Leader</p>");
        html.append("<div style=\"margin-top:16px;padding-top:16px;border-top:1px solid #e2e8f0\">");
        html.append("<p style=\"margin:0;font-size:11px;color:#94a3b8\">");
        html.append("Document ID: ").append(escape(envelopeId));
        html.append("</p></div></div>");

        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        helper.setText(html.toString(), true);
        mailSender.send(mime);
    }

    /**
     * Send completion notification to sender
     */
    public void sendCompletionNotification(String toEmail,
                                          String documentName,
                                          String envelopeId) throws Exception {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

        helper.setTo(toEmail);
        if (fromAddress != null && !fromAddress.isBlank()) {
            helper.setFrom(fromAddress);
        }
        helper.setSubject("Document Completed: " + documentName);

        String safeDocument = documentName != null ? documentName : "Your document";
        String viewLink = baseUrl + "/agreements?id=" + envelopeId;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset=\"UTF-8\"></head>");
        html.append("<body style=\"font-family:Arial,sans-serif;line-height:1.6;color:#0f172a\">");
        html.append("<div style=\"max-width:600px;margin:0 auto;padding:20px\">");
        html.append("<h2 style=\"color:#0d6efd\">✓ Document Completed</h2>");
        html.append("<p>Good news! All recipients have signed <strong>");
        html.append(escape(safeDocument));
        html.append("</strong>.</p>");
        html.append("<p><a href=\"").append(escape(viewLink));
        html.append("\" style=\"display:inline-block;padding:12px 24px;background:#0d6efd;color:#fff;text-decoration:none;border-radius:6px\">View Document</a></p>");
        html.append("<p style=\"font-size:12px;color:#64748b\">Document ID: ");
        html.append(escape(envelopeId));
        html.append("</p>");
        html.append("<p style=\"font-size:13px;color:#64748b\">Thanks,<br/>DocuSign Team</p>");
        html.append("</div></body></html>");

        helper.setText(html.toString(), true);
        mailSender.send(mime);
    }

    /**
     * Send voided notification
     */
    public void sendVoidedNotification(String toEmail,
                                       String recipientName,
                                       String documentName,
                                       String reason) throws Exception {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

        helper.setTo(toEmail);
        if (fromAddress != null && !fromAddress.isBlank()) {
            helper.setFrom(fromAddress);
        }
        helper.setSubject("Document Voided: " + documentName);

        String safeName = recipientName != null ? recipientName : "";
        String safeDocument = documentName != null ? documentName : "The document";
        String safeReason = reason != null ? reason : "No reason provided";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset=\"UTF-8\"></head>");
        html.append("<body style=\"font-family:Arial,sans-serif;line-height:1.6;color:#0f172a\">");
        html.append("<div style=\"max-width:600px;margin:0 auto;padding:20px\">");

        if (!safeName.isBlank()) {
            html.append("<p>Hello ").append(escape(safeName)).append(",</p>");
        } else {
            html.append("<p>Hello,</p>");
        }

        html.append("<p><strong>").append(escape(safeDocument));
        html.append("</strong> has been voided and no longer requires your signature.</p>");
        html.append("<p><strong>Reason:</strong> ").append(escape(safeReason)).append("</p>");
        html.append("<p style=\"font-size:13px;color:#64748b\">Thanks,<br/>DocuSign Team</p>");
        html.append("</div></body></html>");

        helper.setText(html.toString(), true);
        mailSender.send(mime);
    }

    /**
     * Minimal HTML escape for security
     */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
