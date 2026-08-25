package com.accsaber.backend.repository.milestone;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.milestone.MilestoneItem;
import com.accsaber.backend.model.entity.milestone.MilestoneItem.MilestoneItemId;

public interface MilestoneItemRepository extends JpaRepository<MilestoneItem, MilestoneItemId> {

        @Query("""
                        SELECT mi FROM MilestoneItem mi
                        JOIN FETCH mi.item
                        WHERE mi.milestone.id IN :milestoneIds
                        """)
        List<MilestoneItem> findByMilestoneIds(@Param("milestoneIds") Collection<UUID> milestoneIds);

        @Modifying
        @Query("DELETE FROM MilestoneItem mi WHERE mi.milestone.id = :milestoneId")
        int deleteByMilestone_Id(@Param("milestoneId") UUID milestoneId);
}
