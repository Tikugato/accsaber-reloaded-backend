package com.accsaber.backend.repository.campaign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.accsaber.backend.model.entity.campaign.CampaignMilestone;

@Repository
public interface CampaignMilestoneRepository extends JpaRepository<CampaignMilestone, UUID> {

    List<CampaignMilestone> findByCampaign_IdAndActiveTrue(UUID campaignId);

    Optional<CampaignMilestone> findByIdAndActiveTrue(UUID id);
}
