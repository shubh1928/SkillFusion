package com.barterlink.sessions.recording;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.OutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class GoogleDriveStorageService {

    private static final Set<String> SUPPORTED_VIDEO_TYPES =
            Set.of("video/webm", "video/mp4");

    private final GoogleDriveClientFactory driveClientFactory;
    private final String rootFolderId;

    private static final String RECORDING_FOLDER_NAME =
            "SkillFusion Session Recordings";

    private static final String FOLDER_MIME_TYPE =
            "application/vnd.google-apps.folder";

    private volatile String cachedRecordingFolderId;

    public GoogleDriveStorageService(
            GoogleDriveClientFactory driveClientFactory,
            @Value("${barterlink.recording.google-drive.root-folder-id:}")
            String rootFolderId
    ) {
        this.driveClientFactory = driveClientFactory;
        this.rootFolderId = rootFolderId;
    }

    public File uploadRecording(
            InputStream inputStream,
            long fileSizeBytes,
            String fileName,
            String mimeType
    ) throws IOException {

        Objects.requireNonNull(
                inputStream,
                "Recording input stream must not be null"
        );

        try (InputStream recordingStream = inputStream) {

            if (fileSizeBytes <= 0) {
                throw new IllegalArgumentException(
                        "Recording file must not be empty"
                );
            }

            if (!StringUtils.hasText(fileName)) {
                throw new IllegalArgumentException(
                        "Recording file name is required"
                );
            }

            String normalizedMimeType = normalizeMimeType(mimeType);

            if (!SUPPORTED_VIDEO_TYPES.contains(normalizedMimeType)) {
                throw new IllegalArgumentException(
                        "Unsupported recording format. "
                                + "Supported formats are WebM and MP4."
                );
            }

            Drive drive = driveClientFactory.createClient();

            String targetFolderId = resolveRecordingFolderId(drive);

            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setMimeType(normalizedMimeType);
            fileMetadata.setParents(List.of(targetFolderId));

            InputStreamContent content = new InputStreamContent(
                    normalizedMimeType,
                    recordingStream
            );

            content.setLength(fileSizeBytes);

            Drive.Files.Create uploadRequest = drive.files()
                    .create(fileMetadata, content)
                    .setFields(
                            "id,name,mimeType,size,createdTime,webViewLink"
                    );

            uploadRequest.getMediaHttpUploader()
                    .setDirectUploadEnabled(false);

            File uploadedFile = uploadRequest.execute();

            if (uploadedFile == null
                    || !StringUtils.hasText(uploadedFile.getId())) {
                throw new IOException(
                        "Google Drive did not return a recording file ID."
                );
            }

            return uploadedFile;
        }
    }

    private String resolveRecordingFolderId(
            Drive drive
    ) throws IOException {

        // Use an explicitly configured folder ID only if one is supplied.
        if (StringUtils.hasText(rootFolderId)) {
            return rootFolderId.trim();
        }

        // Reuse the folder ID while this backend instance is running.
        if (StringUtils.hasText(cachedRecordingFolderId)) {
            return cachedRecordingFolderId;
        }

        synchronized (this) {

            if (StringUtils.hasText(cachedRecordingFolderId)) {
                return cachedRecordingFolderId;
            }

            // Find a folder previously created by this application.
            String query =
                    "name = '" + RECORDING_FOLDER_NAME + "'"
                            + " and mimeType = '" + FOLDER_MIME_TYPE + "'"
                            + " and trashed = false";

            FileList folders = drive.files()
                    .list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setPageSize(100)
                    .setFields("files(id,name,mimeType)")
                    .execute();

            if (folders.getFiles() != null) {
                for (File folder : folders.getFiles()) {
                    if (StringUtils.hasText(folder.getId())) {
                        cachedRecordingFolderId = folder.getId();
                        return cachedRecordingFolderId;
                    }
                }
            }

            // The folder doesn't exist yet, so create it.
            File folderMetadata = new File();
            folderMetadata.setName(RECORDING_FOLDER_NAME);
            folderMetadata.setMimeType(FOLDER_MIME_TYPE);

            File createdFolder = drive.files()
                    .create(folderMetadata)
                    .setFields("id,name,mimeType")
                    .execute();

            if (createdFolder == null
                    || !StringUtils.hasText(createdFolder.getId())) {
                throw new IOException(
                        "Google Drive did not return the recording folder ID."
                );
            }

            cachedRecordingFolderId = createdFolder.getId();

            return cachedRecordingFolderId;
        }
    }

    private String normalizeMimeType(String mimeType) {

        if (!StringUtils.hasText(mimeType)) {
            throw new IllegalArgumentException(
                    "Recording MIME type is required"
            );
        }

        int parameterIndex = mimeType.indexOf(';');

        String baseMimeType = parameterIndex >= 0
                ? mimeType.substring(0, parameterIndex)
                : mimeType;

        return baseMimeType.trim().toLowerCase(Locale.ROOT);
    }

    public void downloadRecording(
            String googleDriveFileId,
            OutputStream outputStream
    ) throws IOException {

        if (!StringUtils.hasText(googleDriveFileId)) {
            throw new IllegalArgumentException(
                    "Google Drive file ID is required");
        }

        Objects.requireNonNull(
                outputStream,
                "Output stream must not be null"
        );

        Drive drive = driveClientFactory.createClient();

        drive.files()
                .get(googleDriveFileId)
                .executeMediaAndDownloadTo(outputStream);
    }

    String ensureRecordingFolderReady() throws IOException {
        Drive drive = driveClientFactory.createClient();

        String folderId = resolveRecordingFolderId(drive);

        if (!StringUtils.hasText(folderId)) {
            throw new IOException(
                    "Google Drive recording folder could not be found or created."
            );
        }

        return folderId;
    }

}