package com.hubfeatcreators.domain.compliance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policy_aceites")
public class PolicyAceite {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String versao;

    @Column(name = "aceito_em", nullable = false)
    private Instant aceitoEm = Instant.now();

    @Column private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    public PolicyAceite() {}

    public PolicyAceite(UUID userId, String versao, String ip, String userAgent) {
        this.userId = userId;
        this.versao = versao;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getVersao() { return versao; }
    public Instant getAceitoEm() { return aceitoEm; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
}
