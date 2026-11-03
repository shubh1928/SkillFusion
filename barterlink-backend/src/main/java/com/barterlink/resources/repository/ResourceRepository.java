package com.barterlink.resources.repository;

import com.barterlink.auth.entities.User;
import com.barterlink.resources.entity.Resource;
import com.barterlink.resources.enums.ResourceVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository
        extends JpaRepository<Resource, UUID> {

    List<Resource> findByOwnerOrderByCreatedAtDesc(
            User owner
    );

    List<Resource> findByVisibilityOrderByCreatedAtDesc(
            ResourceVisibility visibility
    );

    List<Resource> findByOwnerAndVisibilityOrderByCreatedAtDesc(
            User owner,
            ResourceVisibility visibility
    );


    long countByOwner(
            User owner
    );

    long countByOwnerAndVisibility(
            User owner,
            ResourceVisibility visibility
    );

    @org.springframework.data.jpa.repository.Query("""

        SELECT COALESCE(SUM(r.downloadCount), 0)

        FROM Resource r

        WHERE r.owner = :owner

        """)
    long getTotalDownloadsByOwner(
            @org.springframework.data.repository.query.Param("owner")
            User owner
    );


}