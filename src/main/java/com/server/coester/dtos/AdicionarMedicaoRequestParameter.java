package com.server.coester.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Para adicionar medição com validação
public record AdicionarMedicaoRequestParameter(
        @NotNull(message = "Dimensões são obrigatórias")
        java.util.Map<@NotBlank String, @NotNull Double> dimensoes,

        String observacoes
) {}