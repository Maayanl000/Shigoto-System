package com.shigoto.backend.service;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.NotificationType;
import com.shigoto.backend.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders escaped HTML email content for candidate-facing recruitment notifications.
 */
@Component
public class CandidateEmailRenderer {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, uuuu 'at' h:mm a", Locale.ENGLISH);

    @Value("${shigoto.frontend-url:}")
    private String frontendUrl;

    /**
     * Builds an escaped HTML email and subject for a candidate notification.
     * @param type the requested domain type
     * @param candidate the candidate being processed
     * @param application the application being processed
     * @param interview the interview being processed
     * @return the rendered email subject and escaped HTML body
     */
    public RenderedEmail render(NotificationType type, User candidate, Application application,
                                Interview interview) {
        String jobTitle = escape(application.getJob().getTitle());
        String applicationUrl = applicationUrl(application.getId());
        EventContent content = eventContent(type, application, interview, jobTitle);
        String greeting = candidate.getFirstName() == null || candidate.getFirstName().isBlank()
                ? "Hello," : "Hi " + escape(candidate.getFirstName().trim()) + ",";
        String viewApplication = applicationUrl == null ? "" : button(applicationUrl, "View application");

        String html = """
                <!doctype html><html><body style="margin:0;background:#f3f5f7;font-family:Arial,Helvetica,sans-serif;color:#10233d;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f5f7;padding:24px 12px;"><tr><td align="center">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;background:#ffffff;border:1px solid #e1e7ec;border-radius:8px;overflow:hidden;">
                <tr><td><img src="cid:shigoto-banner" alt="SHIGOTO" width="600" style="display:block;width:100%%;height:auto;border:0;"></td></tr>
                <tr><td style="padding:28px 32px 12px;"><div style="font-size:12px;font-weight:800;letter-spacing:0.2em;color:#087f8c;margin-bottom:20px;">SHIGOTO</div>
                <p style="margin:0 0 18px;font-size:16px;">%s</p>
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.25;color:#10233d;">%s</h1>
                <div style="font-size:15px;line-height:1.65;color:#334155;">%s</div>
                %s
                <div style="margin:24px 0 8px;">%s</div>
                </td></tr>
                <tr><td style="padding:20px 32px;background:#f8fafc;border-top:1px solid #e1e7ec;color:#64748b;font-size:12px;line-height:1.6;">
                <strong>Shigoto - Recruitment made simple</strong><br>This is an automated notification.
                </td></tr></table></td></tr></table></body></html>
                """.formatted(greeting, escape(content.heading()), content.messageHtml(),
                content.detailsHtml(), content.actionHtml() + viewApplication);
        return new RenderedEmail(content.subject(), html);
    }

    /**
     * Selects the subject, message, details, and actions appropriate to the notification type.
     * @param type the requested domain type
     * @param application the application being processed
     * @param interview the interview being processed
     * @param jobTitle the job title
     * @return the content fragments used to assemble the notification email
     */
    private EventContent eventContent(NotificationType type, Application application, Interview interview,
                                      String jobTitle) {
        return switch (type) {
            case APPLICATION_SUBMITTED -> new EventContent("Application received", "Thank you for applying",
                    "We received your application for <strong>" + jobTitle + "</strong> at <strong>"
                            + companyName(application) + "</strong>.<br><br>"
                            + "We will keep you updated as your application progresses.",
                    "", "");
            case HOME_TASK_ASSIGNED -> new EventContent("Home task assigned", "Home task assigned",
                    "A home task was assigned for <strong>" + jobTitle + "</strong>.",
                    details(row("Task instructions", multiline(application.getTaskInstructions()))
                            + row("Deadline", format(application.getTaskDeadline()))), "");
            case HOME_TASK_UPDATED -> new EventContent("Home task deadline updated", "Home task deadline updated",
                    "The home-task deadline for <strong>" + jobTitle + "</strong> has changed.",
                    details(row("Updated deadline", format(application.getTaskDeadline()))), "");
            case INTERVIEW_SCHEDULED -> interviewContent("Interview scheduled", "Your interview for <strong>"
                    + jobTitle + "</strong> has been scheduled.", interview, true);
            case INTERVIEW_RESCHEDULED -> interviewContent("Interview rescheduled", "Your interview for <strong>"
                    + jobTitle + "</strong> has been rescheduled.", interview, true);
            case INTERVIEW_CANCELED -> interviewContent("Interview canceled", "Your interview for <strong>"
                    + jobTitle + "</strong> has been canceled.", interview, false);
            case APPLICATION_OFFERED -> new EventContent("Great news - offer update", "Offer update",
                    "The company would like to move forward with an offer for <strong>" + jobTitle + "</strong>.",
                    "", "");
            case APPLICATION_HIRED -> new EventContent("Congratulations - hiring update", "Congratulations!",
                    "Your application for <strong>" + jobTitle + "</strong> has been marked as hired.",
                    "", "");
            case APPLICATION_REJECTED -> rejectionContent(application, jobTitle);
        };
    }

    /**
     * Returns an HTML-safe company name for email content, with a fallback when no company is assigned.
     * @param application the application being processed
     * @return the escaped company name, or {@code "the company"} when unavailable
     */
    private String companyName(Application application) {
        return application.getJob().getCompany() == null
                ? "the company" : escape(application.getJob().getCompany().getName());
    }

    /**
     * Builds interview-specific email content and includes a join action only when the meeting link is usable.
     * @param heading the heading
     * @param message the exception or response message
     * @param interview the interview being processed
     * @param joinAllowed the join allowed
     * @return email content describing the interview and any permitted join action
     */
    private EventContent interviewContent(String heading, String message, Interview interview, boolean joinAllowed) {
        String detailRows = interview == null ? "" : row("Interview type", displayType(interview))
                + row("Date and time", format(interview.getScheduledAt()));
        String meetingUrl = joinAllowed && interview != null ? validHttpUrl(interview.getMeetingLink()) : null;
        return new EventContent(heading, heading, message, details(detailRows),
                meetingUrl == null ? "" : button(meetingUrl, "Join meeting"));
    }

    /**
     * Builds rejection email content and includes candidate-facing feedback when HR supplied it.
     * @param application the application being processed
     * @param jobTitle the job title
     * @return rejection email content with candidate feedback when available
     */
    private EventContent rejectionContent(Application application, String jobTitle) {
        String feedback = application.getCandidateFeedback();
        if (feedback != null && !feedback.isBlank()) {
            return new EventContent("Application update", "Application update",
                    "Thank you for your interest in <strong>" + jobTitle
                            + "</strong>. After careful consideration, we will not be moving forward with your application.",
                    details(row("Feedback from the recruitment team", multiline(feedback.trim()))), "");
        }
        return new EventContent("Application update", "Application update",
                "Thank you for your interest in <strong>" + jobTitle + "</strong>.<br><br>"
                        + "After careful consideration, we decided to continue with other candidates at this time.<br><br>"
                        + "We appreciate the time you invested in the process and wish you success in your job search.",
                "", "");
    }

    /**
     * Builds the candidate-facing application URL when a valid frontend base URL is configured.
     * @param applicationId the application identifier
     * @return the candidate application URL, or {@code null} when it cannot be built safely
     */
    private String applicationUrl(Long applicationId) {
        String base = validHttpUrl(frontendUrl);
        if (base == null || applicationId == null) return null;
        return base.replaceAll("/+$", "") + "/candidate/applications/" + applicationId;
    }

    /**
     * Accepts only absolute HTTP or HTTPS URLs for optional email links.
     * @param value the value to validate or normalize
     * @return the normalized HTTP(S) URL, or {@code null} when the value is absent or unsafe
     */
    private String validHttpUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = new URI(value.trim());
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null) return uri.toString();
        } catch (URISyntaxException ignored) {
            // Invalid optional links are omitted without preventing the notification email.
        }
        return null;
    }

    /**
     * Converts an interview type to an HTML-safe label suitable for candidate email copy.
     * @param interview the interview being processed
     * @return an escaped, human-readable interview type label
     */
    private String displayType(Interview interview) {
        String value = interview.getType() == null ? "Interview" : switch (interview.getType()) {
            case HR -> "HR interview";
            case TECHNICAL -> "Technical interview";
            case MANAGER -> "Manager interview";
        };
        return escape(value);
    }

    /**
     * Formats an optional interview or task timestamp for display in an HTML email.
     * @param value the value to validate or normalize
     * @return the escaped display timestamp, or {@code "Not provided"} for a missing value
     */
    private String format(LocalDateTime value) {
        return value == null ? "Not provided" : escape(DATE_TIME.format(value));
    }

    /**
     * Escapes free-form text and preserves its line breaks for HTML email display.
     * @param value the value to validate or normalize
     * @return escaped text with line breaks converted to HTML, or a missing-value label
     */
    private String multiline(String value) {
        return value == null || value.isBlank() ? "Not provided"
                : escape(value).replace("\r\n", "<br>").replace("\n", "<br>").replace("\r", "<br>");
    }

    /**
     * Builds one escaped label-value row for an email details table.
     * @param label the label
     * @param value the value to validate or normalize
     * @return an escaped HTML table row for the label and value
     */
    private String row(String label, String value) {
        return "<tr><td style=\"padding:8px 12px;color:#64748b;vertical-align:top;width:34%;\">"
                + escape(label) + "</td><td style=\"padding:8px 12px;color:#10233d;\">" + value + "</td></tr>";
    }

    /**
     * Wraps detail rows in the styled presentation table used by notification emails.
     * @param rows the rows
     * @return an HTML details table, or an empty string when there are no rows
     */
    private String details(String rows) {
        return rows.isEmpty() ? "" : "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" "
                + "style=\"margin-top:20px;background:#f8fafc;border:1px solid #e1e7ec;border-radius:6px;font-size:14px;\">"
                + rows + "</table>";
    }

    /**
     * Builds an HTML-safe call-to-action link for an email.
     * @param url the url
     * @param label the label
     * @return escaped HTML for the call-to-action link
     */
    private String button(String url, String label) {
        return "<a href=\"" + escape(url) + "\" style=\"display:inline-block;margin:0 10px 8px 0;padding:11px 18px;"
                + "background:#087f8c;color:#ffffff;text-decoration:none;border-radius:5px;font-weight:700;font-size:14px;\">"
                + escape(label) + "</a>";
    }

    /**
     * Escapes untrusted text before inserting it into email HTML.
     * @param value the value to validate or normalize
     * @return the value escaped for safe HTML rendering
     */
    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    /**
     * Carries the final email subject and escaped HTML body.
     * @param subject the subject
     * @param html the html
     */
    public record RenderedEmail(String subject, String html) {}
    /**
     * Carries the intermediate content fragments used to assemble an email.
     * @param subject the subject
     * @param heading the heading
     * @param messageHtml the message html
     * @param detailsHtml the details html
     * @param actionHtml the action html
     */
    private record EventContent(String subject, String heading, String messageHtml,
                                String detailsHtml, String actionHtml) {}
}
