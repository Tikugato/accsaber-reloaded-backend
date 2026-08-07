package com.accsaber.backend.model.entity.user;

import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import com.accsaber.backend.model.entity.Category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_category_skills")
@IdClass(UserCategorySkillId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCategorySkill {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "skill_level", nullable = false)
    private double skillLevel;

    @Column(name = "rank_score", nullable = false)
    private double rankScore;

    @Column(name = "sustained_score", nullable = false)
    private double sustainedScore;

    @Column(name = "peak_score", nullable = false)
    private double peakScore;

    @Column(name = "combined_score", nullable = false)
    private double combinedScore;

    @Column(name = "raw_ap_for_one_gain")
    private Double rawApForOneGain;

    @Column(name = "top_ap", nullable = false)
    private double topAp;

    @Column(name = "category_rank")
    private Integer categoryRank;

    @Column(name = "active_players", nullable = false)
    private Long activePlayers;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
