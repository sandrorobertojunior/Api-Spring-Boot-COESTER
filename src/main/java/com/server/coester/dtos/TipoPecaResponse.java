package com.server.coester.dtos;

import java.util.List;

public record TipoPecaResponse(
        Long id,
        String nome,
        String descricao,
        List<CotaMetadata> metadadosCotas
) {
}
