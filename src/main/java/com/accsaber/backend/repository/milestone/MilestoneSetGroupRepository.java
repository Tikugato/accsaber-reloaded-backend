package com.accsaber.backend.repository.milestone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.accsaber.backend.model.entity.milestone.MilestoneSetGroup;

public interface MilestoneSetGroupRepository extends JpaRepository<MilestoneSetGroup, UUID> {

    List<MilestoneSetGroup> findByActiveTrue();

    Optional<MilestoneSetGroup> findByIdAndActiveTrue(UUID id);
}
