package com.server.coester.dtos;

import java.util.List;

public record TipoPecaCompletoResponse(
        Long id,
        String nome,
        String descricao,
        List<CotaMetadata> metadadosCotas,
        EstatisticasTipoPeca estatisticas
) {
    public record EstatisticasTipoPeca(
            Long totalLotes,
            Double taxaAprovacaoMedia,
            Integer medicõesRealizadas
    ) {}
}
