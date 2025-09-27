package com.server.coester.dtos;

import jakarta.validation.constraints.NotBlank;

// Para atualizar lote
public record AtualizarLoteRequest(
        @NotBlank(message = "Descrição é obrigatória")
        String descricao,

        String observacoes
) {}
