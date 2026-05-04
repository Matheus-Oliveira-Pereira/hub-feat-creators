package com.hubfeatcreators.domain.marca;

import com.hubfeatcreators.domain.compliance.BaseLegal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MarcaRequest(
        @NotBlank String nome,
        String segmento,
        String site,
        String observacoes,
        List<String> tags,
        @NotNull BaseLegal baseLegal) {}
