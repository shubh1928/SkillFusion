package com.barterlink.disputes.service;

import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.disputes.dto.response.DisputeCaseReviewResponseDto;
import com.barterlink.disputes.dto.response.DisputeEvidenceItemDto;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.entity.DisputeEvidenceReference;
import com.barterlink.disputes.entity.DisputePanelAssignment;
import com.barterlink.disputes.entity.DisputeVote;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputePanelAssignmentStatus;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import com.barterlink.disputes.repository.DisputeEvidenceReferenceRepository;
import com.barterlink.disputes.repository.DisputePanelAssignmentRepository;
import com.barterlink.disputes.repository.DisputeVoteRepository;
import com.barterlink.disputes.enums.DisputeEvidenceType;
import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.negotiation.entity.NegotiationMessage;
import com.barterlink.sessions.entity.SessionProof;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.disputes.dto.response.AssignedDisputeSummaryDto;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeCaseReviewService {

    private final DisputeCaseRepository disputeCaseRepository;
    private final DisputePanelAssignmentRepository
            disputePanelAssignmentRepository;
    private final DisputeEvidenceReferenceRepository
            disputeEvidenceReferenceRepository;
    private final DisputeVoteRepository disputeVoteRepository;

    @Transactional(readOnly = true)
    public DisputeCaseReviewResponseDto getCaseReview(
            UUID disputeId,
            UUID panelistId) {

        if (disputeId == null || panelistId == null) {
            throw new BusinessException(
                    "Dispute ID and authenticated user are required");
        }

        DisputeCase disputeCase = disputeCaseRepository.findById(disputeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Dispute case not found"));

        if (!isReviewableStatus(disputeCase.getStatus())) {
            throw new BusinessException(
                    "This dispute is not available for panel review");
        }

        DisputePanelAssignment assignment =
                disputePanelAssignmentRepository
                        .findByDisputeCase_Id(disputeId)
                        .stream()
                        .filter(item ->
                                item.getPanelist().getId().equals(panelistId))
                        .findFirst()
                        .orElseThrow(() ->
                                new BusinessException(
                                        "You are not assigned to this dispute panel"));

        if (assignment.getStatus()
                != DisputePanelAssignmentStatus.ASSIGNED
                && assignment.getStatus()
                != DisputePanelAssignmentStatus.VOTED) {

            throw new BusinessException(
                    "Your panel assignment is not eligible to review this dispute");
        }

        int evidenceVersion = disputeCase.getEvidenceVersion() == null
                ? 0
                : disputeCase.getEvidenceVersion();

        DisputeVote currentUserVote =
                disputeVoteRepository
                        .findFirstByDisputeCase_IdAndPanelAssignment_IdAndEvidenceVersionOrderByCreatedAtDesc(
                                disputeId,
                                assignment.getId(),
                                evidenceVersion
                        )
                        .orElse(null);

        List<DisputeEvidenceItemDto> evidence =
                disputeEvidenceReferenceRepository
                        .findByDisputeCase_IdOrderByAttachedAtAsc(disputeId)
                        .stream()
                        .map(this::toEvidenceItem)
                        .toList();

        return DisputeCaseReviewResponseDto.builder()
                .disputeId(disputeCase.getId())
                .sessionId(disputeCase.getSession().getId())
                .agreementId(disputeCase.getSession()
                        .getAgreement().getId())
                .claimantId(disputeCase.getClaimant().getId())
                .claimantName(disputeCase.getClaimant().getName())
                .respondentId(disputeCase.getRespondent().getId())
                .respondentName(disputeCase.getRespondent().getName())
                .reason(disputeCase.getReason())
                .status(disputeCase.getStatus())
                .evidenceVersion(evidenceVersion)
                .createdAt(disputeCase.getCreatedAt())
                .panelAssignedAt(disputeCase.getPanelAssignedAt())
                .decisionOutcome(disputeCase.getDecisionOutcome())
                .decisionSummary(disputeCase.getDecisionSummary())
                .decisionDeclaredAt(disputeCase.getDecisionDeclaredAt())
                .panelSeatNumber(assignment.getSeatNumber())
                .currentUserHasVoted(currentUserVote != null)
                .currentUserVoteOutcome(currentUserVote == null
                        ? null : currentUserVote.getOutcome())
                .currentUserVoteReason(currentUserVote == null
                        ? null : currentUserVote.getReason())
                .currentUserVotedAt(currentUserVote == null
                        ? null : currentUserVote.getCreatedAt())
                .evidence(evidence)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AssignedDisputeSummaryDto> getAssignedDisputes(
            UUID panelistId) {

        if (panelistId == null) {
            throw new BusinessException(
                    "Authenticated panelist ID is required");
        }

        List<DisputePanelAssignment> assignments =
                disputePanelAssignmentRepository
                        .findByPanelist_IdAndStatusInOrderByAssignedAtDesc(
                                panelistId,
                                List.of(
                                        DisputePanelAssignmentStatus.ASSIGNED,
                                        DisputePanelAssignmentStatus.VOTED
                                )
                        );

        return assignments.stream()
                .filter(assignment ->
                        isReviewableStatus(
                                assignment.getDisputeCase().getStatus()))
                .map(this::toAssignedDisputeSummary)
                .toList();
    }

    private AssignedDisputeSummaryDto toAssignedDisputeSummary(
            DisputePanelAssignment assignment) {

        DisputeCase disputeCase = assignment.getDisputeCase();

        return AssignedDisputeSummaryDto.builder()
                .disputeId(disputeCase.getId())
                .sessionId(disputeCase.getSession().getId())
                .agreementId(
                        disputeCase.getSession().getAgreement().getId())
                .claimantName(disputeCase.getClaimant().getName())
                .respondentName(disputeCase.getRespondent().getName())
                .reason(disputeCase.getReason())
                .caseStatus(disputeCase.getStatus())
                .evidenceVersion(disputeCase.getEvidenceVersion())
                .panelSeatNumber(assignment.getSeatNumber())
                .assignmentStatus(assignment.getStatus())
                .createdAt(disputeCase.getCreatedAt())
                .panelAssignedAt(disputeCase.getPanelAssignedAt())
                .decisionOutcome(disputeCase.getDecisionOutcome())
                .decisionSummary(disputeCase.getDecisionSummary())
                .decisionDeclaredAt(disputeCase.getDecisionDeclaredAt())
                .build();
    }

    private boolean isReviewableStatus(DisputeCaseStatus status) {
        return status == DisputeCaseStatus.EVIDENCE_OPEN
                || status == DisputeCaseStatus.VOTING
                || status == DisputeCaseStatus.DECIDED
                || status == DisputeCaseStatus.SETTLEMENT_PENDING
                || status == DisputeCaseStatus.RESOLVED;
    }

    private DisputeEvidenceItemDto toEvidenceItem(
            DisputeEvidenceReference reference) {

        DisputeEvidenceItemDto.DisputeEvidenceItemDtoBuilder builder =
                DisputeEvidenceItemDto.builder()
                        .evidenceReferenceId(reference.getId())
                        .evidenceType(reference.getEvidenceType())
                        .attachedAt(reference.getAttachedAt());

        DisputeEvidenceType type = reference.getEvidenceType();

        if (type == DisputeEvidenceType.SESSION_PROOF) {
            SessionProof proof = reference.getSessionProof();

            if (proof == null) {
                throw new BusinessException(
                        "A session-proof evidence reference is invalid");
            }

            boolean hasFile = proof.getFileName() != null;

            return builder
                    .sourceId(proof.getId())
                    .title(proof.getDescription() == null
                            || proof.getDescription().isBlank()
                            ? "Session proof: " + proof.getProofType()
                            : proof.getDescription())
                    .content(proof.getDescription())
                    .sourceStatus(proof.getProofType().name())
                    .sourceCreatedAt(proof.getCreatedAt())
                    .submittedByName(proof.getUploadedBy().getName())
                    .proofType(proof.getProofType().name())
                    .proofUrl(hasFile
                            ? "/api/v1/session-proofs/"
                                    + proof.getId() + "/content"
                            : proof.getProofUrl())
                    .fileName(proof.getFileName())
                    .contentType(proof.getContentType())
                    .fileSize(proof.getFileSize())
                    .sha256(proof.getSha256())
                    .build();
        }

        if (type == DisputeEvidenceType.NEGOTIATION_MESSAGE) {
            NegotiationMessage message =
                    reference.getNegotiationMessage();

            if (message == null) {
                throw new BusinessException(
                        "A negotiation-message evidence reference is invalid");
            }

            return builder
                    .sourceId(message.getId())
                    .title("Negotiation message: "
                            + message.getMessageType().name())
                    .content(message.getMessage())
                    .sourceStatus(message.getMessageType().name())
                    .sourceCreatedAt(message.getCreatedAt())
                    .submittedByName(message.getSender() == null
                            ? "System"
                            : message.getSender().getName())
                    .attachmentUrl(message.getAttachmentUrl())
                    .systemGenerated(message.getSystemGenerated())
                    .build();
        }

        if (type == DisputeEvidenceType.AGREEMENT_VERSION) {
            AgreementVersion version = reference.getAgreementVersion();

            if (version == null) {
                throw new BusinessException(
                        "An agreement-version evidence reference is invalid");
            }

            return builder
                    .sourceId(version.getId())
                    .title("Agreement version " + version.getVersionNumber())
                    .content(buildAgreementVersionContent(version))
                    .sourceStatus(version.getStatus().name())
                    .sourceCreatedAt(version.getCreatedAt())
                    .submittedByName(version.getCreatedBy().getName())
                    .versionNumber(version.getVersionNumber())
                    .build();
        }

        throw new BusinessException(
                "Unsupported dispute evidence type");
    }

    private String buildAgreementVersionContent(AgreementVersion version) {
        StringBuilder content = new StringBuilder();

        appendField(content, "Offer title", version.getOfferTitle());
        appendField(content, "Offer description", version.getOfferDescription());
        appendField(content, "Request title", version.getRequestTitle());
        appendField(content, "Request description", version.getRequestDescription());
        appendField(content, "Agreed terms", version.getAgreedTerms());
        appendField(content, "Change summary", version.getChangeSummary());

        if (version.getExchangeMode() != null) {
            appendField(content, "Exchange mode",
                    version.getExchangeMode().name());
        }

        if (version.getReleaseMode() != null) {
            appendField(content, "Release mode",
                    version.getReleaseMode().name());
        }

        if (version.getAgreedTokenAmount() != null) {
            appendField(content, "Agreed token amount",
                    version.getAgreedTokenAmount().toString());
        }

        if (version.getEstimatedHours() != null) {
            appendField(content, "Estimated hours",
                    version.getEstimatedHours().toString());
        }

        if (version.getAgreedDeadline() != null) {
            appendField(content, "Agreed deadline",
                    version.getAgreedDeadline().toString());
        }

        return content.toString();
    }

    private void appendField(
            StringBuilder content,
            String label,
            String value) {

        if (value != null && !value.isBlank()) {
            if (!content.isEmpty()) {
                content.append("\n\n");
            }

            content.append(label)
                    .append(": ")
                    .append(value);
        }
    }
}