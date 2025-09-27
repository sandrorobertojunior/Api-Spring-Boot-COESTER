package com.server.coester.controllers;

import com.server.coester.dtos.CriarTipoPecaRequest;
import com.server.coester.dtos.TipoPecaResponse;
import com.server.coester.services.TipoPecaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-peca")
public class TipoPecaController {

    @Autowired
    private TipoPecaService tipoPecaService;

    // CREATE - Criar novo tipo de peça
    @PostMapping
    public ResponseEntity<TipoPecaResponse> criarTipoPeca(@Valid @RequestBody CriarTipoPecaRequest request) {
        try {
            // CORREÇÃO: Use o método correto do service
            TipoPecaResponse response = tipoPecaService.criarTipoPeca(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // READ - Listar todos ordenados por nome
    @GetMapping
    public List<TipoPecaResponse> listarTiposPeca() {
        return tipoPecaService.listarTodosOrdenados();
    }

    // READ - Obter por ID
    @GetMapping("/{id}")
    public ResponseEntity<TipoPecaResponse> obterTipoPeca(@PathVariable Long id) {
        return tipoPecaService.obterPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // READ - Buscar por nome
    @GetMapping("/buscar")
    public List<TipoPecaResponse> buscarTiposPeca(@RequestParam String nome) {
        return tipoPecaService.buscarPorNome(nome);
    }

    // READ - Tipos com lotes
    @GetMapping("/com-lotes")
    public List<TipoPecaResponse> obterTiposComLotes() {
        return tipoPecaService.obterTiposComLotes();
    }

    // UPDATE - Atualizar
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirTipoPeca(@PathVariable Long id) {
        try {
            tipoPecaService.excluirTipoPeca(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ESTATÍSTICAS
    @GetMapping("/estatisticas/uso")
    public ResponseEntity<List<Object[]>> obterEstatisticasUso() {
        try {
            List<Object[]> estatisticas = tipoPecaService.obterEstatisticasUso();
            return ResponseEntity.ok(estatisticas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // TEMPLATE DE MEDIÇÃO
    @GetMapping("/{id}/template-medicao")
    public ResponseEntity<?> getTemplateMedicao(@PathVariable Long id) {
        try {
            // CORREÇÃO: Use o método do service em vez de fazer no controller
            return ResponseEntity.ok(tipoPecaService.getTemplateMedicao(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // HEALTH CHECK
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("TipoPecaController está funcionando!");
    }
}