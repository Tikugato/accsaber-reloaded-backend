package com.accsaber.backend.repository.map;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.map.Map;
import com.accsaber.backend.model.entity.map.MapDifficultyStatus;

public interface MapRepository extends JpaRepository<Map, UUID> {

        Optional<Map> findByIdAndActiveTrue(UUID id);

        Optional<Map> findBySongHashAndActiveTrue(String songHash);

        @Query(value = """
                        SELECT DISTINCT m FROM Map m
                        JOIN m.difficulties d
                        WHERE m.active = true AND d.active = true
                        AND (:categoryId IS NULL OR d.category.id = :categoryId)
                        AND (:status IS NULL OR d.status = :status)
                        AND (CAST(:search AS string) IS NULL OR LOWER(m.songName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                        """, countQuery = """
                        SELECT COUNT(DISTINCT m) FROM Map m
                        JOIN m.difficulties d
                        WHERE m.active = true AND d.active = true
                        AND (:categoryId IS NULL OR d.category.id = :categoryId)
                        AND (:status IS NULL OR d.status = :status)
                        AND (CAST(:search AS string) IS NULL OR LOWER(m.songName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                        """)
        Page<Map> findByDifficultyFilters(
                        @Param("categoryId") UUID categoryId,
                        @Param("status") MapDifficultyStatus status,
                        @Param("search") String search,
                        Pageable pageable);
}
