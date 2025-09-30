package com.server.coester.controllers;

import com.server.coester.dtos.*;
import com.server.coester.entities.Lote;
import com.server.coester.services.LoteService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize; // Import necessário

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    @Autowired
    private LoteService loteService;

    // CREATE - Criar novo lote
    // RESTRITO: APENAS ADMINISTRADOR
    @PostMapping
    public ResponseEntity<LoteResponse> criarLote(@Valid @RequestBody CriarLoteRequest request) {
        try {
            LoteResponse response = loteService.criarLote(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }


    // ⭐ NOVO ENDPOINT: READ - Listar todos os lotes (Apenas Administrador)
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public List<LoteResumidoResponse> listarTodosOsLotes() {
        // Assumindo que LoteService tem um método para listar todos
        return loteService.listarTodosOsLotes();
    }

    // READ - Listar lotes do usuário logado
    // ALTERAÇÃO: Lista apenas os lotes criados pelo usuário autenticado
    @GetMapping
    public List<LoteResumidoResponse> listarLotesDoUsuario() {
        return loteService.listarLotesDoUsuario();
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
    // RESTRITO: APENAS ADMINISTRADOR
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirLote(@PathVariable Long id) {
        try {
            loteService.excluirLote(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            // Se o service lançar AccessDenied (para não-admin/não-dono), retorna 403 Forbidden
            return ResponseEntity.status(403).build();
        } catch (RuntimeException e) {
            // Se a exclusão falhar por ter medições (lógica interna), retorna 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }

    // OPERAÇÕES DE MEDIÇÕES (Mantido sem restrição de acesso por ser operação do colaborador)

    // Adicionar medição ao lote
    @PostMapping("/{loteId}/medicoes")
    public ResponseEntity<LoteResponse> adicionarMedicao(
            @PathVariable Long loteId,
            @Valid @RequestBody AdicionarMedicaoRequestParameter request) { // Usamos o DTO sem pecaNumero
        try {

            // 1. Cria a requisição APENAS com o que é necessário (loteId, dimensoes, observacoes)
            AdicionarMedicaoRequest requestSemPecaNumero = new AdicionarMedicaoRequest(
                    loteId,
                    request.dimensoes(),
                    request.observacoes()
            );

            // 2. O Service agora fará o cálculo do pecaNumero
            LoteResponse response = loteService.adicionarMedicao(requestSemPecaNumero);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Mudar para logar a exceção ou retornar uma mensagem mais clara
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/recomecar")
    public ResponseEntity<LoteResponse> recomecarLote(@PathVariable Long id) {
        LoteResponse response = loteService.recomecarLote(id);
        return ResponseEntity.ok(response);
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

    // OPERAÇÕES DE STATUS (PatchMapping mantido sem restrição)

    // Marcar lote como concluído
    @PatchMapping("/{id}/concluir")
// O ResponseEntity agora retorna um Boolean, representando o resultado da aprovação
    public ResponseEntity<Boolean> concluirLote(@PathVariable Long id) {
        try {
            // Chama o service que agora retorna TRUE (aprovado) ou FALSE (reprovado)
            Boolean aprovado = loteService.concluirLote(id);

            // Retorna 200 OK com o corpo sendo o resultado da aprovação (true ou false)
            return ResponseEntity.ok(aprovado);

        } catch (RuntimeException e) {
            // Se houver RuntimeException (ex: falta de amostras),
            // retorna 400 Bad Request e inclui a mensagem de erro no corpo
            return ResponseEntity.badRequest().header("Error-Message", e.getMessage()).build();
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

    // ALTERAÇÃO: Usa o novo DashboardResponse
    @GetMapping("/dashboard")
    public DashboardResponse obterDashboard() {
        // Assume que o service irá buscar os dados do dashboard do usuário logado
        return loteService.obterDashboard();
    }

    @GetMapping("/estatisticas/periodo")
    public ResponseEntity<List<Object[]>> obterEstatisticasPorPeriodo(
            @RequestParam String dataInicio,
            @RequestParam String dataFim) {
        try {
            // Assume que o service irá buscar os dados do usuário logado
            List<Object[]> estatisticas = loteService.getEstatisticasPorPeriodo(dataInicio, dataFim);
            return ResponseEntity.ok(estatisticas);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}