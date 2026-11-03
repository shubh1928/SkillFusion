
package com.barterlink.notifications.service.impl;

import com.barterlink.notifications.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${barterlink.email.from-address}")
            String fromAddress,
            @Value("${barterlink.email.from-name}")
            String fromName
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void sendEmail(
            String recipient,
            String subject,
            String body
    ) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient email is required"
            );
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "Email subject is required"
            );
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                    "Email body is required"
            );
        }

        try {
            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            false,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(message);

        } catch (
                MessagingException
                | UnsupportedEncodingException exception
        ) {
            throw new IllegalStateException(
                    "Failed to prepare email",
                    exception
            );
        }
    }
}
