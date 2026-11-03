package com.barterlink.marketplace.repository;

import com.barterlink.marketplace.entity.RequestSkill;
import com.barterlink.marketplace.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestSkillRepository
        extends JpaRepository<RequestSkill, UUID> {

    List<RequestSkill> findByRequest(
            ServiceRequest request);

    void deleteByRequest(
            ServiceRequest request);
}