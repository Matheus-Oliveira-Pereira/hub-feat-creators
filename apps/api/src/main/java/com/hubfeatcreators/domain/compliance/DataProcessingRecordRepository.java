package com.hubfeatcreators.domain.compliance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataProcessingRecordRepository extends JpaRepository<DataProcessingRecord, UUID> {
    List<DataProcessingRecord> findByVigenteTrue();
}
