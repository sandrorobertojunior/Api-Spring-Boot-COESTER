package com.server.coester.dtos;

import java.time.LocalDateTime;

// Para listagem resumida de lotes
public record LoteResumidoResponse(
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
        LocalDateTime dataCriacao
) {}
