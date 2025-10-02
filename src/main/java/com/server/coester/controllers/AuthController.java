package com.server.coester.controllers;

import com.server.coester.dtos.UsuarioDto;
import com.server.coester.dtos.UsuarioDtoResume;
import com.server.coester.dtos.UsuarioLoginResponse;
import com.server.coester.entities.Usuario;
import com.server.coester.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    // Registro de usuário
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UsuarioDtoResume usuario) {
        try {
            Usuario novoUsuario = usuarioService.register(usuario);
            return ResponseEntity.ok(novoUsuario);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<UsuarioLoginResponse> login(@RequestBody Usuario usuario) {
        // Armazena a senha em texto puro ANTES de chamar o service.
        // O service usa essa senha para validação contra o hash do DB.
        final String plaintextPassword = usuario.getPassword();

        return usuarioService.login(usuario.getEmail(), plaintextPassword)
                .map(u -> {
                    // 1. Cria e Codifica o token Basic
                    // CRUCIAL: Usa o 'plaintextPassword', não u.getPassword()
                    String token = u.getEmail() + ":" + plaintextPassword;
                    String encodedToken = java.util.Base64.getEncoder().encodeToString(token.getBytes());
                    String basicToken = "Basic " + encodedToken;

                    // 2. Cria o objeto de resposta usando o Record
                    UsuarioLoginResponse response = new UsuarioLoginResponse(
                            basicToken,
                            u.getUsername(),
                            u.getArrayRoles()
                    );

                    // 3. Retorna o objeto JSON 200 OK
                    return ResponseEntity.ok(response);
                })
                // ... (Restante do tratamento de erro)
                .orElse(ResponseEntity.status(401).body(
                        new UsuarioLoginResponse(null, null, null)
                ));
    }

}

