
package com.barterlink.inactivity.repository;

import com.barterlink.inactivity.entity.RetainedUserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RetainedUserIdentityRepository
        extends JpaRepository<RetainedUserIdentity, UUID> {

    Optional<RetainedUserIdentity> findByUser_Id(UUID userId);

    List<RetainedUserIdentity> findByExpiresAtLessThanEqual(
            Instant cutoff
    );


    @Query("""
    SELECT r.user.id
    FROM RetainedUserIdentity r
    WHERE r.expiresAt <= :cutoff
    """)
    List<UUID> findExpiredUserIds(
            @Param("cutoff") Instant cutoff
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT r
    FROM RetainedUserIdentity r
    WHERE r.user.id = :userId
    """)
    Optional<RetainedUserIdentity> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );

}
