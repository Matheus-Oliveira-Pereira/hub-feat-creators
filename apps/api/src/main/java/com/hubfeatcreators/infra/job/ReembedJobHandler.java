package com.hubfeatcreators.infra.job;

import com.hubfeatcreators.domain.match.CreatorFeatureBuilder;
import com.hubfeatcreators.domain.match.CreatorProfileFeatureRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component("REEMBED")
public class ReembedJobHandler implements JobHandler {

    private final CreatorFeatureBuilder featureBuilder;
    private final CreatorProfileFeatureRepository featureRepo;

    public ReembedJobHandler(
            CreatorFeatureBuilder featureBuilder, CreatorProfileFeatureRepository featureRepo) {
        this.featureBuilder = featureBuilder;
        this.featureRepo = featureRepo;
    }

    @Override
    public void handle(Job job) {
        String type = (String) job.getPayload().get("type");
        String idStr = (String) job.getPayload().get("id");
        if (idStr == null) throw new IllegalArgumentException("id ausente no payload");
        UUID id = UUID.fromString(idStr);

        if ("CREATOR".equals(type)) {
            featureRepo
                    .findByInfluenciadorId(id)
                    .ifPresent(f -> featureBuilder.buildAndSave(f.getInfluenciadorId()));
        }
    }
}
