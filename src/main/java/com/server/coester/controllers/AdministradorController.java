package com.server.coester.controllers;

import com.server.coester.dtos.UsuarioDto;
import com.server.coester.services.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdministradorController {

    private final UsuarioService usuarioService;

    public AdministradorController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 1. Endpoint de Teste (mantido)
    @GetMapping("/ping")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Acesso de Administrador concedido via Authority!");
    }

    // 2. Novo Endpoint: Listar Todos os Usuários
    /**
     * Lista todos os usuários do sistema.
     * Mapeamento: GET /api/admin/usuarios
     * Acesso restrito à Authority 'ADMINISTRADOR'.
     *
     * @return Uma lista de objetos UsuarioDTO.
     */
    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<List<UsuarioDto>> getAllUsuarios() {
        try {
            List<UsuarioDto> usuarios = usuarioService.getAllUsuarios();
            System.out.println(usuarios);
            // 1. TRATAMENTO DE LISTA VAZIA: Retorna 204 No Content
            if (usuarios.isEmpty()) {
                // É uma resposta HTTP mais semântica do que 200 OK com corpo vazio
                return ResponseEntity.noContent().build();
            }

            // 2. SUCESSO: Retorna 200 OK com os dados
            return ResponseEntity.ok(usuarios);

        } catch (Exception e) {
            // 3. TRATAMENTO DE ERRO GENÉRICO: Retorna 500 Internal Server Error
            // Adicione logging aqui para registrar o erro no servidor
            System.err.println("Erro ao buscar lista de usuários: " + e.getMessage());

            // Retorna um erro interno, possivelmente com uma mensagem de erro
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}