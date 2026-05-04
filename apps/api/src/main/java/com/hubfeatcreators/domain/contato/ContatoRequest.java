package com.hubfeatcreators.domain.contato;

import com.hubfeatcreators.domain.compliance.BaseLegal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ContatoRequest(
        @NotNull UUID marcaId,
        @NotBlank String nome,
        @Email String email,
        String telefone,
        String cargo,
        @NotNull BaseLegal baseLegal) {}
