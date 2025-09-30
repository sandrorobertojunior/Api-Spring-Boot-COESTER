package com.server.coester.controllers;

import com.server.coester.dtos.CriarTipoPecaRequest;
import com.server.coester.dtos.TipoPecaResponse;
import com.server.coester.services.TipoPecaService;
import com.server.coester.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-peca")
public class TipoPecaController {

    @Autowired
    private TipoPecaService tipoPecaService;


    // CREATE - Criar novo tipo de peça
    // RESTRITO: APENAS ADMINISTRADOR PODE ADICIONAR
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<TipoPecaResponse> criarTipoPeca(@Valid @RequestBody CriarTipoPecaRequest request) {
        try {
            TipoPecaResponse response = tipoPecaService.criarTipoPeca(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // READ - Listar todos ordenados por nome (Permitido a todos)
    @GetMapping
    public List<TipoPecaResponse> listarTiposPeca() {
        return tipoPecaService.listarTodosOrdenados();
    }

    // READ - Obter por ID (Permitido a todos)
    @GetMapping("/{id}")
    public ResponseEntity<TipoPecaResponse> obterTipoPeca(@PathVariable Long id) {
        return tipoPecaService.obterPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // READ - Buscar por nome (Permitido a todos)
    @GetMapping("/buscar")
    public List<TipoPecaResponse> buscarTiposPeca(@RequestParam String nome) {
        return tipoPecaService.buscarPorNome(nome);
    }

    // READ - Tipos com lotes (Permitido a todos)
    @GetMapping("/com-lotes")
    public List<TipoPecaResponse> obterTiposComLotes() {
        return tipoPecaService.obterTiposComLotes();
    }

    // UPDATE - Atualizar (Permitido a todos)
    @PutMapping("/{id}")
    public ResponseEntity<TipoPecaResponse> atualizarTipoPeca(
            @PathVariable Long id,
            @Valid @RequestBody CriarTipoPecaRequest request) {
        try {
            TipoPecaResponse response = tipoPecaService.atualizarTipoPeca(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE - Excluir
    // RESTRITO: APENAS ADMINISTRADOR PODE DELETAR
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirTipoPeca(@PathVariable Long id) {
        try {
            tipoPecaService.excluirTipoPeca(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // Se a exclusão falhar por causa de dependência (Bad Request)
            return ResponseEntity.badRequest().build();
        }
    }

    // ESTATÍSTICAS (Permitido a todos)
    @GetMapping("/estatisticas/uso")
    public ResponseEntity<List<Object[]>> obterEstatisticasUso() {
        try {
            List<Object[]> estatisticas = tipoPecaService.obterEstatisticasUso();
            return ResponseEntity.ok(estatisticas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // TEMPLATE DE MEDIÇÃO (Permitido a todos)
    @GetMapping("/{id}/template-medicao")
    public ResponseEntity<?> getTemplateMedicao(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(tipoPecaService.getTemplateMedicao(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // HEALTH CHECK (Permitido a todos)
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("TipoPecaController está funcionando!");
    }
}