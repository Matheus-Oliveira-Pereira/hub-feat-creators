package com.hubfeatcreators.domain.marca;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record MarcaRequest(
        @NotBlank String nome,
        String segmento,
        String site,
        String observacoes,
        List<String> tags) {}
