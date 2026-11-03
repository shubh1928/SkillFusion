package com.barterlink.marketplace.entity;

import com.barterlink.skills.entity.Skill;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(
        name = "request_skills",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "request_id",
                                "skill_id"
                        }
                )
        }
)
public class RequestSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_skill_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "request_id",
            nullable = false
    )
    private ServiceRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "skill_id",
            nullable = false
    )
    private Skill skill;

    @Builder.Default
    @Column(nullable = false)
    private Boolean preferred = false;
}
