package com.accsaber.backend.repository.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.user.UserCategorySkill;
import com.accsaber.backend.model.entity.user.UserCategorySkillId;

public interface UserCategorySkillRepository extends JpaRepository<UserCategorySkill, UserCategorySkillId> {

        @Query("""
                        SELECT s FROM UserCategorySkill s
                        JOIN FETCH s.category c
                        WHERE s.user.id = :userId AND c.active = true
                        """)
        List<UserCategorySkill> findByUserIdActive(@Param("userId") Long userId);

        @Query("""
                        SELECT s FROM UserCategorySkill s
                        JOIN FETCH s.category c
                        WHERE s.user.id = :userId AND c.id = :categoryId
                        """)
        Optional<UserCategorySkill> findByUserIdAndCategoryId(
                        @Param("userId") Long userId,
                        @Param("categoryId") UUID categoryId);

        @Query("""
                        SELECT s FROM UserCategorySkill s
                        JOIN FETCH s.category c
                        WHERE s.user.id = :userId AND c.countForOverall = true AND c.active = true
                        """)
        List<UserCategorySkill> findByUserIdForOverall(@Param("userId") Long userId);

        @Query("""
                        SELECT s.user.id FROM UserCategorySkill s
                        JOIN s.user u
                        WHERE s.category.id = :categoryId
                          AND u.active = true AND u.banned = false AND u.playerInactive = false
                        """)
        List<Long> findActiveUserIdsByCategoryId(@Param("categoryId") UUID categoryId);

        @Query("""
                        SELECT s.user.id, s.skillLevel FROM UserCategorySkill s
                        WHERE s.category.id = :categoryId
                          AND s.user.id IN :userIds
                          AND s.skillLevel IS NOT NULL
                        """)
        List<Object[]> findSkillLevelsByCategoryAndUserIds(
                        @Param("categoryId") UUID categoryId,
                        @Param("userIds") Collection<Long> userIds);

        @Modifying
        @Query(value = """
                        INSERT INTO user_category_skills (
                            user_id, category_id, skill_level, rank_score, sustained_score,
                            peak_score, combined_score, raw_ap_for_one_gain, top_ap,
                            category_rank, active_players, updated_at)
                        VALUES (
                            :userId, :categoryId, :skillLevel, :rankScore, :sustainedScore,
                            :peakScore, :combinedScore, :rawApForOneGain, :topAp,
                            :categoryRank, :activePlayers, NOW())
                        ON CONFLICT (user_id, category_id) DO UPDATE SET
                            skill_level         = EXCLUDED.skill_level,
                            rank_score          = EXCLUDED.rank_score,
                            sustained_score     = EXCLUDED.sustained_score,
                            peak_score          = EXCLUDED.peak_score,
                            combined_score      = EXCLUDED.combined_score,
                            raw_ap_for_one_gain = EXCLUDED.raw_ap_for_one_gain,
                            top_ap              = EXCLUDED.top_ap,
                            category_rank       = EXCLUDED.category_rank,
                            active_players      = EXCLUDED.active_players,
                            updated_at          = NOW()
                        """, nativeQuery = true)
        int upsert(
                        @Param("userId") Long userId,
                        @Param("categoryId") UUID categoryId,
                        @Param("skillLevel") Double skillLevel,
                        @Param("rankScore") Double rankScore,
                        @Param("sustainedScore") Double sustainedScore,
                        @Param("peakScore") Double peakScore,
                        @Param("combinedScore") Double combinedScore,
                        @Param("rawApForOneGain") Double rawApForOneGain,
                        @Param("topAp") Double topAp,
                        @Param("categoryRank") Integer categoryRank,
                        @Param("activePlayers") Long activePlayers);
}
