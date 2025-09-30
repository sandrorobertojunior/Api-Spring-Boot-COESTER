package com.server.coester.dtos;

import java.time.LocalDateTime;
import java.util.List;

// Resposta do lote com estatísticas
public record LoteResponse(
        Long id,
        String codigoLote,
        String descricao,
        TipoPecaResponse tipoPeca,
        Integer quantidadePecas,
        Integer quantidadeAmostrasDesejada,
        Double porcentagemAmostragem,
        Integer pecasAprovadas,
        Integer pecasReprovadas,
        Double taxaAprovacao,
        String status,
        LocalDateTime dataCriacao,
        List<MedicaoResponse> medicoes
) {}
