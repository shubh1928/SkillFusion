package com.barterlink.skills.repository;

import com.barterlink.auth.entities.User;
import com.barterlink.skills.entity.Skill;
import com.barterlink.skills.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSkillRepository extends JpaRepository<UserSkill, UUID> {

    List<UserSkill> findByUser(User user);

    List<UserSkill> findBySkill(Skill skill);

    Optional<UserSkill> findByUserAndSkill(User user, Skill skill);

    boolean existsByUserAndSkill(User user, Skill skill);

}