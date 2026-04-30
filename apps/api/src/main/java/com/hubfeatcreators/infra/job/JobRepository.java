package com.hubfeatcreators.infra.job;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobRepository extends JpaRepository<Job, UUID> {

  @Query(
      value =
          """
          SELECT * FROM jobs
          WHERE status = 'PENDENTE'
            AND proxima_tentativa_em <= :now
          ORDER BY proxima_tentativa_em
          LIMIT :limit
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  List<Job> pickupPending(Instant now, int limit);
}
