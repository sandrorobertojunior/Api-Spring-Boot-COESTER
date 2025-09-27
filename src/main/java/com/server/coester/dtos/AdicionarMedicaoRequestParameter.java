package com.server.coester.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Para adicionar medição com validação
public record AdicionarMedicaoRequestParameter(
        @NotNull(message = "Número da peça é obrigatório")
        @Positive(message = "Número da peça deve ser positivo")
        Integer pecaNumero,

        @NotNull(message = "Dimensões são obrigatórias")
        java.util.Map<@NotBlank String, @NotNull Double> dimensoes,

        String observacoes
) {}