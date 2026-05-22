package com.hubfeatcreators.domain.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

    private final AdminAuditLogRepository repo;
    private final ObjectMapper objectMapper;

    public AdminAuditService(AdminAuditLogRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    /** Failure to log does NOT abort the operation — logs WARN only. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            UUID adminId,
            String acao,
            UUID assessoriaIdAfetada,
            String recurso,
            UUID recursoId,
            Object payloadAntes,
            Object payloadDepois,
            String ip,
            String userAgent) {
        try {
            String antes = toJson(payloadAntes);
            String depois = toJson(payloadDepois);
            repo.save(
                    new AdminAuditLog(
                            adminId,
                            acao,
                            assessoriaIdAfetada,
                            recurso,
                            recursoId,
                            antes,
                            depois,
                            ip,
                            userAgent));
        } catch (Exception e) {
            log.warn(
                    "admin_audit.log.failed acao={} adminId={}: {}", acao, adminId, e.getMessage());
        }
    }

    public void log(UUID adminId, String acao, String ip, String userAgent) {
        log(adminId, acao, null, null, null, null, null, ip, userAgent);
    }

    public void logTenantBypass(
            UUID adminId,
            String path,
            UUID assessoriaId,
            String method,
            String ip,
            String userAgent) {
        String acao =
                (method != null && !method.equals("GET"))
                        ? "TENANT_BYPASS_WRITE"
                        : "TENANT_BYPASS_READ";
        log(adminId, acao, assessoriaId, path, null, null, null, ip, userAgent);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
