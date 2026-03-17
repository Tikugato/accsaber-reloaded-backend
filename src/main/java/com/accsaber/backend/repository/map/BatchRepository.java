package com.accsaber.backend.repository.map;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accsaber.backend.model.entity.map.Batch;
import com.accsaber.backend.model.entity.map.BatchStatus;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

        @Query(value = """
                        SELECT b FROM Batch b
                        WHERE (CAST(:search AS string) IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))
                        """, countQuery = """
                        SELECT COUNT(b) FROM Batch b
                        WHERE (CAST(:search AS string) IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))
                        """)
        Page<Batch> findAllWithSearch(
                        @Param("search") String search, Pageable pageable);

        @Query(value = """
                        SELECT b FROM Batch b
                        WHERE b.status = :status
                        AND (CAST(:search AS string) IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))
                        """, countQuery = """
                        SELECT COUNT(b) FROM Batch b
                        WHERE b.status = :status
                        AND (CAST(:search AS string) IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))
                        """)
        Page<Batch> findByStatusWithSearch(
                        @Param("status") BatchStatus status,
                        @Param("search") String search,
                        Pageable pageable);

        @Query(value = """
                        SELECT b FROM Batch b
                        LEFT JOIN b.difficulties d ON d.active = true
                        WHERE (CAST(:search AS string) IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))
                        GROUP BY b
                        """, countQuery = """
                        SELECT COUNT(b) FROM Batch b
                        WHERE (CAST(:search AS string) IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))
                        """)
        Page<Batch> findAllWithDifficultyCount(
                        @Param("search") String search, Pageable pageable);

        @Query(value = """
                        SELECT b FROM Batch b
                        LEFT JOIN b.difficulties d ON d.active = true
                        WHERE b.status = :status
                        AND (CAST(:search AS string) IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))
                        GROUP BY b
                        """, countQuery = """
                        SELECT COUNT(b) FROM Batch b
                        WHERE b.status = :status
                        AND (CAST(:search AS string) IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))
                        """)
        Page<Batch> findByStatusWithDifficultyCount(
                        @Param("status") BatchStatus status,
                        @Param("search") String search,
                        Pageable pageable);
}
