package com.barterlink.sessions.recording;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
public class GoogleDriveClientFactory {

    private final String applicationName;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;

    public GoogleDriveClientFactory(
            @Value("${barterlink.recording.google-drive.application-name:SkillFusion}")
            String applicationName,

            @Value("${barterlink.recording.google-drive.client-id:}")
            String clientId,

            @Value("${barterlink.recording.google-drive.client-secret:}")
            String clientSecret,

            @Value("${barterlink.recording.google-drive.refresh-token:}")
            String refreshToken
    ) {
        this.applicationName = applicationName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    public Drive createClient() throws IOException {
        validateConfiguration();

        try {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();

            return new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName(applicationName)
                    .build();

        } catch (GeneralSecurityException exception) {
            throw new IOException(
                    "Failed to initialize the Google Drive HTTP transport.",
                    exception
            );
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(clientId)
                || !StringUtils.hasText(clientSecret)
                || !StringUtils.hasText(refreshToken)) {

            throw new IllegalStateException(
                    "Google Drive credentials are not configured. "
                            + "Set GOOGLE_DRIVE_CLIENT_ID, "
                            + "GOOGLE_DRIVE_CLIENT_SECRET, and "
                            + "GOOGLE_DRIVE_REFRESH_TOKEN."
            );
        }
    }
}