package com.server.coester.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Para criar lote com validação
public record CriarLoteRequest(
        @NotBlank(message = "Descrição é obrigatória")
        String descricao,

        @NotNull(message = "Tipo de peça é obrigatório")
        @Positive(message = "ID do tipo de peça deve ser positivo")
        Long tipoPecaId,

        @NotNull(message = "Quantidade de peças é obrigatória")
        @Positive(message = "Quantidade deve ser positiva")
        Integer quantidadePecas,

        Integer quantidadeAmostrasDesejada,

        String observacoes
) {}