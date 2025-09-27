package com.server.coester.dtos;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CriarTipoPecaRequest(
        @NotBlank String nome,
        String descricao,
        List<CotaMetadata> dimensoes  // ← Agora recebe a lista diretamente
) {}

