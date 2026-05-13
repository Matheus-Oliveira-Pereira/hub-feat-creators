package com.hubfeatcreators.domain.match;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchFeedbackRepository extends JpaRepository<MatchFeedback, UUID> {

    List<MatchFeedback> findBySugestaoId(UUID sugestaoId);
}
