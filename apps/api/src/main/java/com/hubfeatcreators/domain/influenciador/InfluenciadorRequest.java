package com.hubfeatcreators.domain.influenciador;

import com.hubfeatcreators.domain.compliance.BaseLegal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record InfluenciadorRequest(
        @NotBlank String nome,
        Map<String, String> handles,
        String nicho,
        Long audienciaTotal,
        String observacoes,
        List<String> tags,
        @NotNull BaseLegal baseLegal) {}
