package com.accsaber.backend.repository.campaign;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.accsaber.backend.model.entity.campaign.CampaignRewardTotals;

public interface CampaignRewardTotalsRepository extends JpaRepository<CampaignRewardTotals, UUID> {

    List<CampaignRewardTotals> findByCampaignIdIn(Collection<UUID> campaignIds);
}
