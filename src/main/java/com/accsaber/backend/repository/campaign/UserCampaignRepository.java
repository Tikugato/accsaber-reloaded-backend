package com.accsaber.backend.repository.campaign;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.campaign.CampaignStatus;
import com.accsaber.backend.model.entity.campaign.UserCampaign;
import com.accsaber.backend.model.entity.campaign.UserCampaignStatus;

public interface UserCampaignRepository extends JpaRepository<UserCampaign, UUID> {

    Optional<UserCampaign> findByUser_IdAndCampaign_IdAndActiveTrue(Long userId, UUID campaignId);

    List<UserCampaign> findByUser_IdAndCampaign_IdInAndActiveTrue(Long userId, Collection<UUID> campaignIds);

    List<UserCampaign> findByUser_IdAndStatusInAndActiveTrue(Long userId, Collection<UserCampaignStatus> statuses);

    @Query("""
            SELECT uc FROM UserCampaign uc
            JOIN FETCH uc.campaign
            WHERE uc.campaign.id = :campaignId AND uc.active = true AND uc.status IN :statuses
            """)
    List<UserCampaign> findByCampaignAndStatuses(@Param("campaignId") UUID campaignId,
            @Param("statuses") Collection<UserCampaignStatus> statuses);

    @Query("""
            SELECT DISTINCT uc.user.id FROM UserCampaign uc
            WHERE uc.active = true AND uc.status IN :statuses
              AND uc.campaign.active = true AND uc.campaign.status <> :excludedCampaignStatus
            """)
    List<Long> findUserIdsByStatusesAndCampaignReleased(@Param("statuses") Collection<UserCampaignStatus> statuses,
            @Param("excludedCampaignStatus") CampaignStatus excludedCampaignStatus);

    @Query("""
            SELECT DISTINCT uc.user.id FROM UserCampaign uc
            WHERE uc.campaign.id = :campaignId AND uc.active = true AND uc.status IN :statuses
            """)
    List<Long> findUserIdsByCampaignAndStatuses(@Param("campaignId") UUID campaignId,
            @Param("statuses") Collection<UserCampaignStatus> statuses);
}
