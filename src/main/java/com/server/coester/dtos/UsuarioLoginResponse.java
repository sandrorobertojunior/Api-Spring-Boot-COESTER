package com.server.coester.dtos;

import java.util.List;

public record UsuarioLoginResponse(
        String basicToken,
        String nome,
        List<String> roles
) {}
