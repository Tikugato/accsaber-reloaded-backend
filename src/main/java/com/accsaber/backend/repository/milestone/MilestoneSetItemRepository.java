package com.accsaber.backend.repository.milestone;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.milestone.MilestoneSetItem;
import com.accsaber.backend.model.entity.milestone.MilestoneSetItem.MilestoneSetItemId;

public interface MilestoneSetItemRepository extends JpaRepository<MilestoneSetItem, MilestoneSetItemId> {

        @Query("""
                        SELECT si FROM MilestoneSetItem si
                        JOIN FETCH si.item
                        WHERE si.milestoneSet.id IN :setIds
                        """)
        List<MilestoneSetItem> findBySetIds(@Param("setIds") Collection<UUID> setIds);

        @Modifying
        @Query("DELETE FROM MilestoneSetItem si WHERE si.milestoneSet.id = :setId")
        int deleteByMilestoneSet_Id(@Param("setId") UUID setId);
}
