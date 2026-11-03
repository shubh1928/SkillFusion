package com.barterlink.agreements.repository;

import com.barterlink.agreements.entity.AgreementVersionMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgreementVersionMilestoneRepository
        extends JpaRepository<AgreementVersionMilestone, UUID> {

    List<AgreementVersionMilestone> findByVersion_IdOrderByMilestoneNumberAsc(
            UUID versionId);

    Optional<AgreementVersionMilestone> findByVersion_IdAndMilestoneNumber(
            UUID versionId,
            Integer milestoneNumber);
}