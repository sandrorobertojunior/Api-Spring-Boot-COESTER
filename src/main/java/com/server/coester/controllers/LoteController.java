package com.server.coester.controllers;

import com.server.coester.dtos.*;
import com.server.coester.services.LoteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    @Autowired
    private LoteService loteService;

    // CREATE - Criar novo lote
    @PostMapping
    public ResponseEntity<LoteResponse> criarLote(@Valid @RequestBody CriarLoteRequest request) {
        try {
            LoteResponse response = loteService.criarLote(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null); // ← Retornar body nulo ou mensagem
        }
    }

    // READ - Listar todos os lotes (resumido)
    @GetMapping
    public List<LoteResumidoResponse> listarLotes() {
        return loteService.listarLotesResumido();
    }

    // READ - Obter lote específico com detalhes
    @GetMapping("/{id}")
    public ResponseEntity<LoteResponse> obterLote(@PathVariable Long id) {
        return loteService.obterLotePorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // READ - Listar lotes por status
    @GetMapping("/status/{status}")
    public List<LoteResumidoResponse> listarLotesPorStatus(@PathVariable String status) {
        return loteService.listarLotesPorStatus(status);
    }

    // READ - Buscar lotes por descrição
    @GetMapping("/buscar")
    public List<LoteResumidoResponse> buscarLotes(@RequestParam String descricao) {
        return loteService.buscarLotesPorDescricao(descricao);
    }

    // UPDATE - Atualizar lote
    @PutMapping("/{id}")
    public ResponseEntity<LoteResponse> atualizarLote(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarLoteRequest request) {
        try {
            LoteResponse response = loteService.atualizarLote(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE - Excluir lote
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirLote(@PathVariable Long id) {
        try {
            loteService.excluirLote(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // OPERAÇÕES DE MEDIÇÕES

    // Adicionar medição ao lote - MELHORADO
    @PostMapping("/{loteId}/medicoes")
    public ResponseEntity<LoteResponse> adicionarMedicao(
            @PathVariable Long loteId,
            @Valid @RequestBody AdicionarMedicaoRequestParameter request) {
        try {
            // Simplificado - usar apenas o loteId do path
            AdicionarMedicaoRequest requestComLoteId = new AdicionarMedicaoRequest(
                    loteId, // ← Usa do path, ignora do request body se existir
                    request.pecaNumero(),
                    request.dimensoes(),
                    request.observacoes()
            );

            LoteResponse response = loteService.adicionarMedicao(requestComLoteId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Listar medições de um lote
    @GetMapping("/{loteId}/medicoes")
    public ResponseEntity<List<MedicaoResponse>> listarMedicoes(@PathVariable Long loteId) {
        try {
            List<MedicaoResponse> medicoes = loteService.listarMedicoes(loteId);
            return ResponseEntity.ok(medicoes);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Remover medição específica
    @DeleteMapping("/{loteId}/medicoes/{medicaoId}")
    public ResponseEntity<LoteResponse> removerMedicao(
            @PathVariable Long loteId,
            @PathVariable Long medicaoId) {
        try {
            LoteResponse response = loteService.removerMedicao(loteId, medicaoId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // OPERAÇÕES DE STATUS

    // Marcar lote como concluído
    @PatchMapping("/{id}/concluir")
    public ResponseEntity<LoteResponse> concluirLote(@PathVariable Long id) {
        try {
            LoteResponse response = loteService.concluirLote(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Reabrir lote
    @PatchMapping("/{id}/reabrir")
    public ResponseEntity<LoteResponse> reabrirLote(@PathVariable Long id) {
        try {
            LoteResponse response = loteService.reabrirLote(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // DASHBOARD E ESTATÍSTICAS

    @GetMapping("/dashboard")
    public DashboardResponse obterDashboard() {
        return loteService.obterDashboard();
    }

    @GetMapping("/estatisticas/periodo")
    public ResponseEntity<List<Object[]>> obterEstatisticasPorPeriodo( // ← ResponseEntity
                                                                       @RequestParam String dataInicio,
                                                                       @RequestParam String dataFim) {
        try {
            List<Object[]> estatisticas = loteService.getEstatisticasPorPeriodo(dataInicio, dataFim);
            return ResponseEntity.ok(estatisticas);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}