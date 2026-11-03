
package com.barterlink.sessions.service.impl;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.sessions.dto.request.UploadProofRequestDto;
import com.barterlink.sessions.dto.response.SessionProofResponseDto;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.entity.SessionProof;
import com.barterlink.sessions.entity.SessionProofContent;
import com.barterlink.sessions.enums.ProofType;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import com.barterlink.sessions.repository.SessionProofContentRepository;
import com.barterlink.sessions.repository.SessionProofRepository;
import com.barterlink.sessions.service.SessionProofService;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.entity.DisputeEvidenceReference;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputeEvidenceType;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import com.barterlink.disputes.repository.DisputeEvidenceReferenceRepository;
import lombok.RequiredArgsConstructor;
import com.barterlink.disputes.enums.DisputePanelAssignmentStatus;
import com.barterlink.disputes.repository.DisputePanelAssignmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.barterlink.sessions.dto.response.SessionProofFileDownloadDto;
import com.barterlink.auth.services.UserActivityService;
import com.barterlink.rewards.enums.RewardQualifyingActivityType;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionProofServiceImpl implements SessionProofService {

    private final SessionProofRepository proofRepository;
    private final SessionProofContentRepository contentRepository;
    private final ServiceSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final DisputeCaseRepository disputeCaseRepository;
    private final DisputeEvidenceReferenceRepository disputeEvidenceReferenceRepository;
    private final DisputePanelAssignmentRepository disputePanelAssignmentRepository;
    private final UserActivityService userActivityService;

    @Value("${barterlink.session-proofs.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    @Override
    public SessionProofResponseDto uploadProof(
            UUID sessionId,
            UUID userId,
            MultipartFile file,
            UploadProofRequestDto request) {

        if (request == null || request.getProofType() == null) {
            throw new BusinessException("Proof type is required");
        }

        ServiceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Session not found"));

        if (session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessException(
                    "Proof cannot be uploaded for this session");
        }

        ensureSessionParticipant(session, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        ProofType proofType = request.getProofType();

        boolean hasFile = file != null && !file.isEmpty();

        String requestedUrl = request.getProofUrl();
        boolean hasUrl = requestedUrl != null
                && !requestedUrl.isBlank();

        String externalUrl = null;
        ValidatedAttachment attachment = null;

        if (isLinkType(proofType)) {

            if (hasFile) {
                throw new BusinessException(
                        "This proof type requires a link, not a file");
            }

            if (!hasUrl) {
                throw new BusinessException(
                        "A URL is required for this proof type");
            }

            externalUrl = validateExternalUrl(requestedUrl);

        } else if (proofType == ProofType.OTHER) {

            if (hasFile == hasUrl) {
                throw new BusinessException(
                        "For OTHER, provide exactly one file or one URL");
            }

            if (hasUrl) {
                externalUrl = validateExternalUrl(requestedUrl);
            } else {
                attachment = validateAttachment(proofType, file);
            }

        } else {

            if (!hasFile) {
                throw new BusinessException(
                        "A file is required for this proof type");
            }

            if (hasUrl) {
                throw new BusinessException(
                        "Do not provide a URL when uploading a file");
            }

            attachment = validateAttachment(proofType, file);
        }

        SessionProof.SessionProofBuilder proofBuilder =
                SessionProof.builder()
                        .session(session)
                        .uploadedBy(user)
                        .proofType(proofType)
                        .proofUrl(externalUrl)
                        .description(request.getDescription());

        if (attachment != null) {
            proofBuilder
                    .fileName(attachment.fileName())
                    .contentType(attachment.contentType())
                    .fileSize((long) attachment.bytes().length)
                    .sha256(attachment.sha256());
        }

        SessionProof savedProof =
                proofRepository.save(proofBuilder.build());

        if (attachment != null) {
            SessionProofContent content = SessionProofContent.builder()
                    .proof(savedProof)
                    .fileData(attachment.bytes())
                    .build();

            contentRepository.save(content);
        }

// Automatically attach new evidence when a dispute is open.
        attachProofToActiveDispute(session, savedProof);

        userActivityService.recordQualifyingActivity(
                userId,
                RewardQualifyingActivityType.SESSION_PROOF_SUBMITTED,
                savedProof.getId().toString()
        );

        return mapToResponse(savedProof);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionProofResponseDto> getSessionProofs(
            UUID sessionId,
            UUID userId) {

        ServiceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Session not found"));

        ensureSessionEvidenceAccess(session, userId);

        return proofRepository
                .findBySessionOrderByCreatedAtAsc(session)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }



    @Override
    @Transactional(readOnly = true)
    public SessionProofFileDownloadDto getProofContent(
            UUID proofId,
            UUID userId) {

        SessionProof proof = proofRepository.findById(proofId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Proof not found"));

        ensureSessionEvidenceAccess(proof.getSession(), userId);

        if (proof.getFileName() == null
                || proof.getSha256() == null
                || proof.getContentType() == null) {
            throw new BusinessException(
                    "This proof does not contain an uploaded file");
        }

        SessionProofContent content = contentRepository.findById(proofId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Proof content not found"));

        byte[] fileData = content.getFileData();

        if (fileData == null
                || !proof.getSha256().equals(calculateSha256(fileData))) {
            throw new BusinessException(
                    "Stored proof content failed integrity verification");
        }

        return new SessionProofFileDownloadDto(
                fileData,
                proof.getFileName(),
                proof.getContentType()
        );
    }


    @Override
    public void deleteProof(UUID proofId, UUID userId) {

        SessionProof proof = proofRepository.findById(proofId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Proof not found"));

        if (!proof.getUploadedBy().getId().equals(userId)) {
            throw new BusinessException(
                    "Only the uploader can delete this proof");
        }

        if (proof.getSession().getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessException(
                    "Completed session proofs cannot be deleted");
        }

        if (proof.getSession().getStatus() == SessionStatus.DISPUTED) {
            throw new BusinessException(
                    "Session proofs cannot be deleted while the session is disputed");
        }

        // Remove attachment bytes before removing their metadata.
        if (proof.getFileName() != null) {
            contentRepository.deleteById(proof.getId());
        }

        proofRepository.delete(proof);
    }

    private void attachProofToActiveDispute(
            ServiceSession session,
            SessionProof proof) {

        // Ordinary sessions do not need dispute-evidence references.
        if (session.getStatus() != SessionStatus.DISPUTED) {
            return;
        }

        DisputeCase disputeCase =
                disputeCaseRepository
                        .findFirstBySession_IdOrderByCreatedAtDesc(
                                session.getId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "The dispute case could not be found"));

        DisputeCaseStatus status = disputeCase.getStatus();

        if (status != DisputeCaseStatus.EVIDENCE_OPEN
                && status != DisputeCaseStatus.VOTING) {

            throw new BusinessException(
                    "Additional evidence can only be submitted before the panel decision is declared");
        }

        boolean alreadyAttached =
                disputeEvidenceReferenceRepository
                        .existsByDisputeCase_IdAndSessionProof_Id(
                                disputeCase.getId(),
                                proof.getId()
                        );

        if (alreadyAttached) {
            return;
        }

        DisputeEvidenceReference reference =
                DisputeEvidenceReference.builder()
                        .disputeCase(disputeCase)
                        .evidenceType(DisputeEvidenceType.SESSION_PROOF)
                        .sessionProof(proof)
                        .build();

        disputeEvidenceReferenceRepository.save(reference);

        int currentVersion = disputeCase.getEvidenceVersion() == null
                ? 0
                : disputeCase.getEvidenceVersion();

        disputeCase.setEvidenceVersion(currentVersion + 1);

        disputeCaseRepository.save(disputeCase);
    }

    private void ensureSessionEvidenceAccess(
            ServiceSession session,
            UUID userId) {

        UUID requesterId = session.getAgreement()
                .getRequester()
                .getId();

        UUID providerId = session.getAgreement()
                .getProvider()
                .getId();

        // Participants retain their existing proof access.
        if (requesterId.equals(userId)
                || providerId.equals(userId)) {
            return;
        }

        DisputeCase disputeCase =
                disputeCaseRepository
                        .findFirstBySession_IdOrderByCreatedAtDesc(
                                session.getId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "You are not authorized to access these proofs"));

        DisputeCaseStatus status = disputeCase.getStatus();

        boolean caseEvidenceReadable =
                status == DisputeCaseStatus.EVIDENCE_OPEN
                        || status == DisputeCaseStatus.VOTING
                        || status == DisputeCaseStatus.DECIDED
                        || status == DisputeCaseStatus.SETTLEMENT_PENDING
                        || status == DisputeCaseStatus.RESOLVED;

        if (!caseEvidenceReadable) {
            throw new BusinessException(
                    "This dispute is not available for panel review");
        }

        boolean assignedPanelist =
                disputePanelAssignmentRepository
                        .existsByDisputeCase_IdAndPanelist_IdAndStatusIn(
                                disputeCase.getId(),
                                userId,
                                List.of(
                                        DisputePanelAssignmentStatus.ASSIGNED,
                                        DisputePanelAssignmentStatus.VOTED
                                )
                        );

        if (!assignedPanelist) {
            throw new BusinessException(
                    "You are not an assigned panel member for this dispute");
        }
    }

    private void ensureSessionParticipant(
            ServiceSession session,
            UUID userId) {

        UUID requesterId = session.getAgreement()
                .getRequester()
                .getId();

        UUID providerId = session.getAgreement()
                .getProvider()
                .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {
            throw new BusinessException(
                    "You are not part of this session");
        }
    }

    private boolean isLinkType(ProofType proofType) {
        return proofType == ProofType.GITHUB
                || proofType == ProofType.GOOGLE_DRIVE
                || proofType == ProofType.WEBSITE;
    }

    private String validateExternalUrl(String value) {

        String url = value.trim();

        if (url.length() > 2000) {
            throw new BusinessException("The URL is too long");
        }

        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();

            boolean validScheme = scheme != null
                    && (scheme.equalsIgnoreCase("http")
                    || scheme.equalsIgnoreCase("https"));

            if (!validScheme
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new BusinessException(
                        "Provide a valid HTTP or HTTPS URL");
            }

            return uri.toASCIIString();

        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    "Provide a valid HTTP or HTTPS URL");
        }
    }

    private ValidatedAttachment validateAttachment(
            ProofType proofType,
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    "A non-empty file is required");
        }

        if (maxFileSizeBytes <= 0) {
            throw new IllegalStateException(
                    "The session-proof file-size limit must be positive");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new BusinessException(
                    "The attachment exceeds the maximum allowed size");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String extension = getExtension(fileName);

        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(
                    "Unable to read the uploaded attachment");
        }

        if (bytes.length == 0) {
            throw new BusinessException("The uploaded file is empty");
        }

        if (bytes.length > maxFileSizeBytes) {
            throw new BusinessException(
                    "The attachment exceeds the maximum allowed size");
        }

        String contentType = validateFileType(
                proofType,
                extension,
                bytes
        );

        return new ValidatedAttachment(
                fileName,
                contentType,
                bytes,
                calculateSha256(bytes)
        );
    }

    private String validateFileType(
            ProofType proofType,
            String extension,
            byte[] bytes) {

        switch (proofType) {

            case PDF:
                if (!extension.equals("pdf")
                        || !startsWith(bytes, new byte[]{
                        '%', 'P', 'D', 'F', '-'
                })) {
                    throw new BusinessException(
                            "Only valid PDF attachments are allowed");
                }
                return "application/pdf";

            case ZIP:
                if (!extension.equals("zip") || !isZipSignature(bytes)) {
                    throw new BusinessException(
                            "Only ZIP attachments are allowed");
                }
                return "application/zip";

            case IMAGE:
                if (extension.equals("jpg")
                        || extension.equals("jpeg")) {

                    if (!startsWith(bytes, new byte[]{
                            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
                    })) {
                        throw new BusinessException(
                                "The file is not a valid JPEG image");
                    }

                    return "image/jpeg";
                }

                if (extension.equals("png")) {
                    if (!startsWith(bytes, new byte[]{
                            (byte) 0x89, 'P', 'N', 'G',
                            (byte) 0x0D, (byte) 0x0A,
                            (byte) 0x1A, (byte) 0x0A
                    })) {
                        throw new BusinessException(
                                "The file is not a valid PNG image");
                    }

                    return "image/png";
                }

                throw new BusinessException(
                        "Only JPEG and PNG images are allowed");

            case OTHER:
                return validateOtherAttachment(extension, bytes);

            case GITHUB:
            case GOOGLE_DRIVE:
            case WEBSITE:
                throw new BusinessException(
                        "This proof type requires an external URL");

            default:
                throw new BusinessException(
                        "Unsupported proof type");
        }
    }

    private String validateOtherAttachment(
            String extension,
            byte[] bytes) {

        switch (extension) {

            case "docx":
                if (!isZipSignature(bytes)) {
                    throw new BusinessException(
                            "The file does not appear to be a DOCX document");
                }
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

            case "xlsx":
                if (!isZipSignature(bytes)) {
                    throw new BusinessException(
                            "The file does not appear to be an XLSX document");
                }
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            case "txt":
            case "csv":
            case "java":
            case "py":
            case "js":
            case "ts":
            case "html":
            case "css":
            case "sql":
                validateUtf8Text(bytes);
                return extension.equals("csv")
                        ? "text/csv"
                        : "text/plain";

            default:
                throw new BusinessException(
                        "Unsupported attachment format for OTHER");
        }
    }

    private void validateUtf8Text(byte[] bytes) {

        for (byte value : bytes) {
            if (value == 0) {
                throw new BusinessException(
                        "Binary files are not allowed as text attachments");
            }
        }

        try {
            StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));

        } catch (CharacterCodingException ex) {
            throw new BusinessException(
                    "Text attachments must use valid UTF-8 encoding");
        }
    }

    private boolean isZipSignature(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == 'P'
                && bytes[1] == 'K'
                && (
                (bytes[2] == 3 && bytes[3] == 4)
                        || (bytes[2] == 5 && bytes[3] == 6)
                        || (bytes[2] == 7 && bytes[3] == 8)
        );
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {

        if (bytes.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }

        return true;
    }

    private String sanitizeFileName(String originalName) {

        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(
                    "The uploaded file must have a filename");
        }

        String name = originalName.replace('\\', '/');

        int slashIndex = name.lastIndexOf('/');
        if (slashIndex >= 0) {
            name = name.substring(slashIndex + 1);
        }

        name = name.replaceAll("[\\p{Cntrl}]", "");
        name = name.replaceAll("[^\\p{L}\\p{N}._ ()-]", "_");
        name = name.replaceAll("^\\.+", "").trim();

        if (name.isBlank()) {
            throw new BusinessException(
                    "The uploaded filename is invalid");
        }

        if (name.length() > 255) {
            int dotIndex = name.lastIndexOf('.');
            String extension = dotIndex > 0
                    ? name.substring(dotIndex)
                    : "";

            if (extension.length() >= 255) {
                extension = "";
            }

            int baseLength = 255 - extension.length();

            name = name.substring(0, dotIndex > 0
                    ? Math.min(dotIndex, baseLength)
                    : Math.min(name.length(), 255 - extension.length()))
                    + extension;
        }

        return name;
    }

    private String getExtension(String fileName) {

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            throw new BusinessException(
                    "The attachment must have an allowed file extension");
        }

        return fileName.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String calculateSha256(byte[] bytes) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is not available", ex);
        }
    }

    private SessionProofResponseDto mapToResponse(SessionProof proof) {

        SessionProofResponseDto dto = new SessionProofResponseDto();

        dto.setProofId(proof.getId());
        dto.setUploadedById(proof.getUploadedBy().getId());
        dto.setUploadedByName(proof.getUploadedBy().getName());
        dto.setProofType(proof.getProofType());

        boolean isAttachment = proof.getFileName() != null;

        dto.setFileAttachment(isAttachment);
        dto.setFileName(proof.getFileName());
        dto.setContentType(proof.getContentType());
        dto.setFileSize(proof.getFileSize());
        dto.setSha256(proof.getSha256());

        if (isAttachment) {
            dto.setProofUrl(
                    "/api/v1/session-proofs/"
                            + proof.getId()
                            + "/content"
            );
        } else {
            dto.setProofUrl(proof.getProofUrl());
        }

        dto.setDescription(proof.getDescription());
        dto.setCreatedAt(proof.getCreatedAt());

        return dto;
    }

    private record ValidatedAttachment(
            String fileName,
            String contentType,
            byte[] bytes,
            String sha256
    ) {
    }
}
