package com.accsaber.backend.model.entity.campaign;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "campaign_reward_totals")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRewardTotals {

    @Id
    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "total_xp", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalXp;

    @Column(name = "total_rewards", nullable = false)
    private Integer totalRewards;
}
