package com.server.coester.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CotaMetadata(
        @NotBlank String nome,
        @NotBlank String label,
        String tipo,           // "number", "text", "select"
        String unidade,        // "mm", "cm", "kg"
        @Positive Double tolerancia,
        @Positive Double valorPadrao
) {}

