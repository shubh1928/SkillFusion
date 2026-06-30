package com.barterlink.auth.entities;

import com.barterlink.skills.entity.UserSkill;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.barterlink.wallet.entity.Wallet;
import java.time.Instant;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;
    @Column(name = "user_email", unique = true, length = 300)
    private String email;
    @Column(name = "user_name", length = 500)
    private String name;
    private String password;
    private String image;
    private boolean enable = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    private Provider provider = Provider.LOCAL;
    private String providerId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();


    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserSkill> userSkills = new HashSet<>();

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Wallet wallet;

    @PrePersist
    protected void onCreate(){
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = Instant.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
       return roles.stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enable;
    }

    // new added

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Builder.Default
    @Column(nullable = false)
    private Double reputationScore = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Integer completedServices = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer totalRatings = 0;
}
