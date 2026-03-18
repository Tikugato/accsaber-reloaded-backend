package com.accsaber.backend.repository.badge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.accsaber.backend.model.entity.badge.Badge;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    List<Badge> findByActiveTrue();

    Optional<Badge> findByIdAndActiveTrue(UUID id);

    Optional<Badge> findByNameAndActiveTrue(String name);
}
