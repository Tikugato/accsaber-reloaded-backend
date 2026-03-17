package com.accsaber.backend.repository.user;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.accsaber.backend.model.entity.user.MergeScoreAction;

@Repository
public interface MergeScoreActionRepository extends JpaRepository<MergeScoreAction, UUID> {

    List<MergeScoreAction> findByLink_Id(UUID linkId);

    void deleteByLink_Id(UUID linkId);
}
