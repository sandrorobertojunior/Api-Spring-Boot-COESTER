package com.server.coester.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CriarTipoPecaRequest(
        @NotBlank String nome,
        String descricao,
        @NotEmpty
        List<CotaMetadata> dimensoes  // ← Agora recebe a lista diretamente
) {}

