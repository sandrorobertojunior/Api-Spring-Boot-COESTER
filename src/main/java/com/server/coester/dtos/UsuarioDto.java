package com.server.coester.dtos;

import java.util.List;

public record UsuarioDto(Long id,
                         String username,
                         String email,
                         String password) {
}
