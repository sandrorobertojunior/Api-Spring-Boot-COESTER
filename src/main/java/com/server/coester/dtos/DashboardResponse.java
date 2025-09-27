package com.server.coester.dtos;

import java.util.List;

// Resposta de estatísticas do dashboard
public record DashboardResponse(
        Long totalLotes,
        Long lotesEmAndamento,
        Long lotesConcluidos,
        Double taxaAprovacaoGeral,
        List<LoteResumidoResponse> lotesRecentes
) {}
