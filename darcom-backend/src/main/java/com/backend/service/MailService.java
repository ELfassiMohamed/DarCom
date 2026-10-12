package com.backend.service;

import com.backend.config.AppConfig;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Outgoing mail over SMTP (Gmail: host {@code smtp.gmail.com}, port 465 SSL).
 * Only {@code jakarta.mail} API types are referenced; the Angus Mail
 * implementation is bundled in the WAR (Tomcat ships none).
 * Sending is skipped with a warning when no SMTP credentials are configured.
 */
@Singleton
public class MailService {

    private static final Logger LOG = Logger.getLogger(MailService.class.getName());

    private final AppConfig config;
    private volatile Session session;

    @Inject
    public MailService(AppConfig config) {
        this.config = config;
    }

    public boolean isConfigured() {
        return config.getSmtpUsername() != null && !config.getSmtpUsername().isBlank()
                && config.getSmtpPassword() != null && !config.getSmtpPassword().isBlank();
    }

    public void sendVerificationEmail(String to, String fullName, String rawToken) {
        String link = config.getAppPublicUrl() + "/verify-email?token=" + rawToken;
        String name = fullName != null ? fullName : "there";
        String body = "Hi " + name + ",\n\n"
                + "Thanks for registering on DarCom. Please verify your email address:\n\n"
                + link + "\n\n"
                + "If the link does not work, send this token to POST /api/auth/verify-email:\n\n"
                + rawToken + "\n\n"
                + "This link expires in " + config.getVerificationTokenTtlHours() + " hours. "
                + "If you did not create this account, just ignore this email.\n";
        send(to, "Verify your DarCom email", body);
    }

    public void sendPasswordResetEmail(String to, String fullName, String rawToken) {
        String link = config.getAppPublicUrl() + "/reset-password?token=" + rawToken;
        String name = fullName != null ? fullName : "there";
        String body = "Hi " + name + ",\n\n"
                + "We received a request to reset your DarCom password:\n\n"
                + link + "\n\n"
                + "If the link does not work, send this token together with your new password "
                + "to POST /api/auth/reset-password:\n\n"
                + rawToken + "\n\n"
                + "This link expires in " + config.getPasswordResetTokenTtlMinutes() + " minutes. "
                + "If you did not request this, just ignore this email.\n";
        send(to, "Reset your DarCom password", body);
    }

    private void send(String to, String subject, String body) {
        if (!isConfigured()) {
            LOG.warning("SMTP credentials not configured; skipping email to " + to);
            return;
        }
        try {
            MimeMessage message = new MimeMessage(session());
            message.setFrom(new InternetAddress(config.getSmtpFrom(), "DarCom"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            message.setSentDate(new Date());
            Transport.send(message);
            LOG.info("Sent email '" + subject + "' to " + to);
        } catch (MessagingException e) {
            throw new ApiException(500, ErrorCode.INTERNAL_ERROR, "Failed to send email", e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new ApiException(500, ErrorCode.INTERNAL_ERROR, "Failed to send email", e);
        }
    }

    private Session session() {
        Session result = session;
        if (result == null) {
            synchronized (this) {
                result = session;
                if (result == null) {
                    Properties props = new Properties();
                    props.put("mail.smtp.host", config.getSmtpHost());
                    props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
                    props.put("mail.smtp.auth", "true");
                    if (config.getSmtpPort() == 465) {
                        props.put("mail.smtp.ssl.enable", "true");
                    } else {
                        props.put("mail.smtp.starttls.enable", "true");
                    }
                    props.put("mail.smtp.connectiontimeout", "10000");
                    props.put("mail.smtp.timeout", "10000");
                    result = Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(config.getSmtpUsername(), config.getSmtpPassword());
                        }
                    });
                    session = result;
                }
            }
        }
        return result;
    }
}
