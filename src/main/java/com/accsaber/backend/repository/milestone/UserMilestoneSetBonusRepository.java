package com.accsaber.backend.repository.milestone;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.milestone.UserMilestoneSetBonus;

public interface UserMilestoneSetBonusRepository extends JpaRepository<UserMilestoneSetBonus, UUID> {

    boolean existsByUser_IdAndMilestoneSet_Id(Long userId, UUID milestoneSetId);

    @Query("""
            SELECT COALESCE(SUM(umsb.milestoneSet.setBonusXp), 0) FROM UserMilestoneSetBonus umsb
            WHERE umsb.user.id = :userId
            """)
    double sumSetBonusXpByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT COALESCE(SUM(ms.set_bonus_xp), 0)
            FROM user_milestone_set_bonuses umsb
            JOIN milestone_sets ms ON umsb.milestone_set_id = ms.id
            WHERE umsb.user_id = :userId
            AND umsb.claimed_at >= NOW() - INTERVAL '24 hours'
            """, nativeQuery = true)
    double sumSetBonusXpGainedLast24h(@Param("userId") Long userId);

    @Query(value = """
            SELECT uml.user_id, m.set_id, MAX(COALESCE(uml.completed_at, uml.created_at)) AS earned_at
            FROM user_milestone_links uml
            JOIN milestones m ON m.id = uml.milestone_id
            JOIN users u ON u.id = uml.user_id
            WHERE uml.completed = true AND u.active = true
              AND m.active = true AND m.status = 'ACTIVE'
            GROUP BY uml.user_id, m.set_id
            HAVING COUNT(*) = (
                    SELECT COUNT(*) FROM milestones m2
                    WHERE m2.set_id = m.set_id AND m2.active = true AND m2.status = 'ACTIVE')
               AND NOT EXISTS (
                    SELECT 1 FROM user_milestone_set_bonuses b
                    WHERE b.user_id = uml.user_id AND b.milestone_set_id = m.set_id)
            ORDER BY earned_at, uml.user_id
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findUnclaimedSetCompletions(@Param("limit") int limit);

    @Query(value = """
            SELECT b.user_id, b.milestone_set_id, b.claimed_at
            FROM user_milestone_set_bonuses b
            JOIN users u ON u.id = b.user_id
            WHERE u.active = true
              AND EXISTS (
                  SELECT 1 FROM milestone_set_items si
                  JOIN items i ON i.id = si.item_id
                  WHERE si.set_id = b.milestone_set_id
                    AND i.active = true AND i.deprecated = false
                    AND (i.obtainable_until IS NULL OR i.obtainable_until > NOW())
                    AND (i.value IS NULL OR i.value ->> 'grant' IS DISTINCT FROM 'active_crate')
                    AND NOT EXISTS (
                        SELECT 1 FROM user_item_links l
                        WHERE l.user_id = b.user_id AND l.item_id = si.item_id
                          AND l.source = 'milestone_set'
                          AND l.source_id = CAST(b.milestone_set_id AS text)))
            ORDER BY b.claimed_at, b.user_id
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findBonusesMissingRewards(@Param("limit") int limit);
}
