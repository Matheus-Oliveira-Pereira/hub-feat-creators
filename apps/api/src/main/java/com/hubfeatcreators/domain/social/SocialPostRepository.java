package com.hubfeatcreators.domain.social;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialPostRepository extends JpaRepository<SocialPost, UUID> {

    Optional<SocialPost> findBySocialAccountIdAndExternalPostId(
            UUID socialAccountId, String externalPostId);

    List<SocialPost> findTop30BySocialAccountIdOrderByPostedAtDesc(UUID socialAccountId);
}
