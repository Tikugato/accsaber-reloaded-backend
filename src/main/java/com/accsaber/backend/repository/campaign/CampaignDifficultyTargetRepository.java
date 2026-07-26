package com.accsaber.backend.repository.campaign;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.campaign.CampaignDifficultyTarget;

public interface CampaignDifficultyTargetRepository extends JpaRepository<CampaignDifficultyTarget, UUID> {

    List<CampaignDifficultyTarget> findByCampaignDifficulty_IdOrderByOrdinalAsc(UUID campaignDifficultyId);

    @Query("""
            SELECT t FROM CampaignDifficultyTarget t
            WHERE t.campaignDifficulty.id IN :campaignDifficultyIds
            ORDER BY t.ordinal ASC
            """)
    List<CampaignDifficultyTarget> findByCampaignDifficultyIds(
            @Param("campaignDifficultyIds") Collection<UUID> campaignDifficultyIds);

    @Query("""
            SELECT t FROM CampaignDifficultyTarget t
            WHERE t.campaignDifficulty.campaign.id = :campaignId
              AND t.campaignDifficulty.active = true
            ORDER BY t.ordinal ASC
            """)
    List<CampaignDifficultyTarget> findByCampaign_Id(@Param("campaignId") UUID campaignId);

    void deleteByCampaignDifficulty_Id(UUID campaignDifficultyId);
}
