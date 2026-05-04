package com.hubfeatcreators.domain.influenciador;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record InfluenciadorRequest(
        @NotBlank String nome,
        Map<String, String> handles,
        String nicho,
        Long audienciaTotal,
        String observacoes,
        List<String> tags) {}
