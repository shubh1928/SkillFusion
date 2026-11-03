package com.barterlink.disputes.service;

import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.agreements.repository.AgreementVersionRepository;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.entity.DisputeEvidenceReference;
import com.barterlink.disputes.enums.DisputeEvidenceType;
import com.barterlink.disputes.repository.DisputeEvidenceReferenceRepository;
import com.barterlink.negotiation.entity.NegotiationMessage;
import com.barterlink.negotiation.repository.NegotiationMessageRepository;
import com.barterlink.sessions.entity.SessionProof;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.repository.SessionProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisputeEvidenceService {

    private final DisputeEvidenceReferenceRepository evidenceReferenceRepository;
    private final SessionProofRepository sessionProofRepository;
    private final NegotiationMessageRepository negotiationMessageRepository;
    private final AgreementVersionRepository agreementVersionRepository;

    @Transactional
    public List<DisputeEvidenceReference> attachExistingEvidence(
            DisputeCase disputeCase) {

        if (disputeCase == null || disputeCase.getId() == null) {
            throw new IllegalArgumentException(
                    "A saved dispute case is required.");
        }

        ServiceSession session = disputeCase.getSession();

        if (session == null || session.getAgreement() == null) {
            throw new IllegalStateException(
                    "The dispute must be linked to a session and agreement.");
        }

        var negotiation = session.getAgreement().getNegotiation();

        if (negotiation == null || negotiation.getId() == null) {
            throw new IllegalStateException(
                    "The session agreement must have a negotiation.");
        }

        List<DisputeEvidenceReference> references = new ArrayList<>();

        // 1. Attach existing session proofs.
        List<SessionProof> proofs =
                sessionProofRepository.findBySessionOrderByCreatedAtAsc(
                        session);

        for (SessionProof proof : proofs) {
            boolean alreadyAttached =
                    evidenceReferenceRepository
                            .existsByDisputeCase_IdAndSessionProof_Id(
                                    disputeCase.getId(),
                                    proof.getId());

            if (!alreadyAttached) {
                references.add(
                        DisputeEvidenceReference.builder()
                                .disputeCase(disputeCase)
                                .evidenceType(
                                        DisputeEvidenceType.SESSION_PROOF)
                                .sessionProof(proof)
                                .build()
                );
            }
        }

        // 2. Attach negotiation history, including system-generated messages.
        List<NegotiationMessage> messages =
                negotiationMessageRepository
                        .findByNegotiationOrderByCreatedAtAsc(
                                negotiation);

        for (NegotiationMessage message : messages) {
            boolean alreadyAttached =
                    evidenceReferenceRepository
                            .existsByDisputeCase_IdAndNegotiationMessage_Id(
                                    disputeCase.getId(),
                                    message.getId());

            if (!alreadyAttached) {
                references.add(
                        DisputeEvidenceReference.builder()
                                .disputeCase(disputeCase)
                                .evidenceType(
                                        DisputeEvidenceType.NEGOTIATION_MESSAGE)
                                .negotiationMessage(message)
                                .build()
                );
            }
        }

        // 3. Attach all agreement versions, newest first.
        List<AgreementVersion> versions =
                agreementVersionRepository
                        .findByNegotiation_IdOrderByVersionNumberDesc(
                                negotiation.getId());

        for (AgreementVersion version : versions) {
            boolean alreadyAttached =
                    evidenceReferenceRepository
                            .existsByDisputeCase_IdAndAgreementVersion_Id(
                                    disputeCase.getId(),
                                    version.getId());

            if (!alreadyAttached) {
                references.add(
                        DisputeEvidenceReference.builder()
                                .disputeCase(disputeCase)
                                .evidenceType(
                                        DisputeEvidenceType.AGREEMENT_VERSION)
                                .agreementVersion(version)
                                .build()
                );
            }
        }

        if (references.isEmpty()) {
            return List.of();
        }

        return evidenceReferenceRepository.saveAll(references);
    }
}