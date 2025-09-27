package com.server.coester.controllers;

import com.server.coester.entities.Usuario;
import com.server.coester.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Registro de usuário
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Usuario usuario) {
        try {
            Usuario novoUsuario = usuarioService.register(usuario);
            return ResponseEntity.ok(novoUsuario);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Usuario usuario) {
        return usuarioService.login(usuario.getEmail(), usuario.getPassword())
                .map(u -> {
                    // Cria o token Basic (usuario:senha em Base64)
                    String token = usuario.getEmail() + ":" + usuario.getPassword();
                    String encodedToken = java.util.Base64.getEncoder().encodeToString(token.getBytes());
                    return ResponseEntity.ok("Basic " + encodedToken);
                })
                .orElse(ResponseEntity.status(401).body("Usuário ou senha inválidos"));
    }

}

