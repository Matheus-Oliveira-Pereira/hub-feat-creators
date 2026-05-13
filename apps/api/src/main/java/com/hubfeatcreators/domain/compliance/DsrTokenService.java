package com.hubfeatcreators.domain.compliance;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles DsrToken persistence in an isolated transaction so token consumption is durable even if
 * the outer DSR action fails and triggers a rollback.
 */
@Service
public class DsrTokenService {

    private final DsrTokenRepository tokenRepo;

    public DsrTokenService(DsrTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DsrToken markUsed(DsrToken token) {
        token.setUsedAt(Instant.now());
        return tokenRepo.save(token);
    }
}
