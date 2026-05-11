package com.hubfeatcreators.domain.importacao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportJobLinhaRepository extends JpaRepository<ImportJobLinha, ImportJobLinhaId> {

    Page<ImportJobLinha> findByIdJobId(UUID jobId, Pageable pageable);

    @Query(
            """
      SELECT l FROM ImportJobLinha l
      WHERE l.id.jobId = :jobId AND l.status = :status
      ORDER BY l.id.linha ASC
      """)
    List<ImportJobLinha> findByJobIdAndStatus(
            @Param("jobId") UUID jobId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM ImportJobLinha l WHERE l.id.jobId = :jobId")
    void deleteByJobId(@Param("jobId") UUID jobId);
}
