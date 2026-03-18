package com.accsaber.backend.repository.badge;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.accsaber.backend.model.entity.badge.UserBadgeLink;

@Repository
public interface UserBadgeLinkRepository extends JpaRepository<UserBadgeLink, UUID> {

    List<UserBadgeLink> findByUser_Id(Long userId);

    boolean existsByUser_IdAndBadge_Id(Long userId, UUID badgeId);
}
