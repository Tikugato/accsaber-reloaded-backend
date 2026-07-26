package com.accsaber.backend.repository.campaign;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.campaign.CampaignDifficultyModifier;
import com.accsaber.backend.model.entity.campaign.CampaignDifficultyModifier.CampaignDifficultyModifierId;

public interface CampaignDifficultyModifierRepository
        extends JpaRepository<CampaignDifficultyModifier, CampaignDifficultyModifierId> {

    @Query("""
            SELECT cdm FROM CampaignDifficultyModifier cdm
            JOIN FETCH cdm.modifier
            WHERE cdm.campaignDifficulty.id = :campaignDifficultyId
            """)
    List<CampaignDifficultyModifier> findByCampaignDifficulty_Id(
            @Param("campaignDifficultyId") UUID campaignDifficultyId);

    @Query("""
            SELECT cdm FROM CampaignDifficultyModifier cdm
            JOIN FETCH cdm.modifier
            WHERE cdm.campaignDifficulty.id IN :campaignDifficultyIds
            """)
    List<CampaignDifficultyModifier> findByCampaignDifficulty_IdIn(
            @Param("campaignDifficultyIds") Collection<UUID> campaignDifficultyIds);

    @Query("""
            SELECT cdm FROM CampaignDifficultyModifier cdm
            JOIN FETCH cdm.modifier
            WHERE cdm.campaignDifficulty.campaign.id = :campaignId
              AND cdm.campaignDifficulty.active = true
            """)
    List<CampaignDifficultyModifier> findByCampaign_Id(@Param("campaignId") UUID campaignId);

    void deleteByCampaignDifficulty_Id(UUID campaignDifficultyId);
}
