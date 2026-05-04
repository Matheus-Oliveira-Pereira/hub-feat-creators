package com.hubfeatcreators.domain.onboarding;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {
    @Id
    @Column(name = "key", nullable = false)
    private String key;

    @Column(nullable = false)
    private int count = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public LoginAttempt() {}

    public LoginAttempt(String key) {
        this.key = key;
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public String getKey() { return key; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
