package pep.com.pepclass.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendOtpEmail(String toEmail, String otpCode, int expiryMinutes) {
        String subject = "MediCare Plus — Password Reset Verification Code [" + otpCode + "]";
        String htmlBody = """
            <div style="font-family: 'Plus Jakarta Sans', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 32px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;">
                <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 24px;">
                    <div style="width: 36px; height: 36px; background: #2563eb; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-weight: 800; text-align: center; line-height: 36px;">♥</div>
                    <span style="font-weight: 800; font-size: 1.2rem; color: #0f172a;">MediCare Plus</span>
                </div>
                <h2 style="color: #0f172a; font-size: 1.5rem; margin-top: 0;">Password Reset Request</h2>
                <p style="color: #475467; font-size: 1rem; line-height: 1.6;">
                    You have requested to reset your password for your MediCare Plus account (<strong>%s</strong>).
                </p>
                <div style="background-color: #eff6ff; border: 1px solid #dbeafe; border-radius: 12px; padding: 24px; text-align: center; margin: 24px 0;">
                    <span style="display: block; font-size: 0.85rem; font-weight: 700; color: #1e40af; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 8px;">Your 6-Digit Verification Code</span>
                    <strong style="font-size: 2.2rem; font-weight: 800; color: #2563eb; letter-spacing: 6px;">%s</strong>
                    <span style="display: block; font-size: 0.85rem; color: #64748b; margin-top: 10px;">Valid for <strong>%d minutes</strong>. Do not share this code with anyone.</span>
                </div>
                <p style="color: #64748b; font-size: 0.9rem; line-height: 1.5;">
                    If you did not request a password reset, please ignore this email. Your account remains secure.
                </p>
                <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 32px 0 20px;">
                <p style="color: #94a3b8; font-size: 0.8rem; text-align: center; margin: 0;">
                    © 2026 MediCare Plus Hospital Management System. All rights reserved.
                </p>
            </div>
            """.formatted(toEmail, otpCode, expiryMinutes);

        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom("no-reply@medicareplus.com");
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
                log.info("[SMTP SUCCESS] Sent OTP email to {}", toEmail);
                return true;
            } catch (Exception e) {
                log.error("[SMTP ERROR] Failed to send email to {}: {}", toEmail, e.getMessage(), e);
                logConsoleFallback(toEmail, otpCode);
                return true;
            }
        } else {
            logConsoleFallback(toEmail, otpCode);
            return true;
        }
    }

    private void logConsoleFallback(String toEmail, String otpCode) {
        log.info("=================================================");
        log.info("[EMAIL DISPATCH TO: {}]", toEmail);
        log.info("OTP VERIFICATION CODE: {}", otpCode);
        log.info("VALID FOR: 5 MINUTES");
        log.info("=================================================");
    }
}
