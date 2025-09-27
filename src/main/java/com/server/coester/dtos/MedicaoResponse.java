package com.server.coester.dtos;

import java.time.LocalDateTime;
import java.util.Map;

// Dados de uma medição individual
public record MedicaoResponse(
        Long id,
        LocalDateTime data,
        Integer pecaNumero,
        Map<String, Double> dimensoes,
        String status,
        String observacoes
) {}
