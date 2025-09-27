package com.server.coester.dtos;

public record TipoPecaEstatisticasResponse(
        String nomeTipoPeca,
        Long totalLotes,
        Long totalMedicoes
) {}
