package com.hubfeatcreators.domain.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, String> {
    Optional<LoginAttempt> findByKey(String key);

    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.key = :key")
    void clearByKey(String key);
}
