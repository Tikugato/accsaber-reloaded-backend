package com.accsaber.backend.repository.user;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.user.UserPinnedMilestone;

public interface UserPinnedMilestoneRepository extends JpaRepository<UserPinnedMilestone, UUID> {

    @Query("""
            SELECT p FROM UserPinnedMilestone p
            JOIN FETCH p.milestone m
            JOIN FETCH m.milestoneSet
            LEFT JOIN FETCH m.category
            WHERE p.user.id = :userId AND m.active = true
            ORDER BY p.displayOrder ASC
            """)
    List<UserPinnedMilestone> findActiveByUserIdWithMilestoneGraph(@Param("userId") Long userId);

    void deleteByUser_Id(Long userId);
}
