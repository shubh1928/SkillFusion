
package com.barterlink.notifications.service;

public interface EmailService {

    void sendEmail(
            String recipient,
            String subject,
            String body
    );
}
