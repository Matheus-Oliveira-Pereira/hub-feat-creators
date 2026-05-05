package com.hubfeatcreators.domain.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notificacao_dedupe")
public class NotificacaoDedupe {

    @Id private String key;

    @Column(name = "last_emitted", nullable = false)
    private Instant lastEmitted;

    protected NotificacaoDedupe() {}

    public NotificacaoDedupe(String key, Instant lastEmitted) {
        this.key = key;
        this.lastEmitted = lastEmitted;
    }

    public String getKey() { return key; }
    public Instant getLastEmitted() { return lastEmitted; }
    public void setLastEmitted(Instant lastEmitted) { this.lastEmitted = lastEmitted; }
}
