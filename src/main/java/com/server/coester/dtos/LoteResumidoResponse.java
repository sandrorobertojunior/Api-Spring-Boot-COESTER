package com.server.coester.dtos;

import java.time.LocalDateTime;

// Para listagem resumida de lotes
public record LoteResumidoResponse(
        Long id,
        String codigoLote,
        String descricao,
        String tipoPecaNome,
        Integer quantidadePecas,
        Integer quantidadeAmostras,
        Double taxaAprovacao,
        String status,
        LocalDateTime dataCriacao
) {}
