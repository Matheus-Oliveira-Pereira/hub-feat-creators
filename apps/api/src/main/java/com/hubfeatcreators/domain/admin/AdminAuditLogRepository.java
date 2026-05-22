package com.hubfeatcreators.domain.admin;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    @Query("SELECT a FROM AdminAuditLog a ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findAllPaged(Pageable pageable);

    @Query("SELECT a FROM AdminAuditLog a WHERE a.adminId = :adminId ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findByAdminId(UUID adminId, Pageable pageable);

    @Query("SELECT a FROM AdminAuditLog a WHERE a.acao = :acao ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findByAcao(String acao, Pageable pageable);

    @Query(
            "SELECT a FROM AdminAuditLog a WHERE a.assessoriaIdAfetada = :assessoriaId ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findByAssessoriaIdAfetada(UUID assessoriaId, Pageable pageable);
}
