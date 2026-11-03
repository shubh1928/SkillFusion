
package com.barterlink.sessions.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "session_proof_contents")
public class SessionProofContent {

    @Id
    @Column(
            name = "proof_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "proof_id",
            nullable = false
    )
    private SessionProof proof;

    @Lob
    @Column(
            name = "file_data",
            nullable = false,
            columnDefinition = "LONGBLOB"
    )
    private byte[] fileData;
}
