package com.server.coester.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.coester.dtos.CotaMetadata;
import com.server.coester.dtos.CriarTipoPecaRequest;
import com.server.coester.dtos.TipoPecaResponse;
import com.server.coester.entities.TipoPeca;
import com.server.coester.repositories.TipoPecaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TipoPecaService {

    @Autowired
    private TipoPecaRepository tipoPecaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public TipoPecaResponse criarTipoPeca(CriarTipoPecaRequest request) {
        if (tipoPecaRepository.existsByNome(request.nome())) {
            throw new RuntimeException("Tipo de peça já existe");
        }

        TipoPeca tipoPeca = new TipoPeca();
        tipoPeca.setNome(request.nome());
        tipoPeca.setDescricao(request.descricao());
        tipoPeca.setMetadadosCotas(convertDimensoesToJson(request.dimensoes()));

        TipoPeca saved = tipoPecaRepository.save(tipoPeca);
        return toTipoPecaResponse(saved);
    }

    private String convertDimensoesToJson(List<CotaMetadata> dimensoes) {
        try {
            Map<String, Object> metadados = new HashMap<>();
            metadados.put("dimensoes", dimensoes);
            return objectMapper.writeValueAsString(metadados);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter dimensões para JSON", e);
        }
    }

    public List<TipoPecaResponse> listarTodos() {
        return tipoPecaRepository.findAll().stream()
                .map(this::toTipoPecaResponse)
                .toList();
    }

    public Optional<TipoPecaResponse> obterPorId(Long id) {
        return tipoPecaRepository.findById(id)
                .map(this::toTipoPecaResponse);
    }

    public List<TipoPecaResponse> buscarPorNome(String nome) {
        return tipoPecaRepository.findByNomeContainingIgnoreCase(nome).stream()
                .map(this::toTipoPecaResponse)
                .toList();
    }

    public List<TipoPecaResponse> obterTiposComLotes() {
        return tipoPecaRepository.findTiposComLotes().stream()
                .map(this::toTipoPecaResponse)
                .toList();
    }

    // CORREÇÃO: Este método deve receber o DTO, não a entidade
    public TipoPecaResponse atualizarTipoPeca(Long id, CriarTipoPecaRequest request) {
        TipoPeca tipoPeca = tipoPecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de peça não encontrado"));

        // Verificar se novo nome já existe (exceto para o próprio registro)
        if (!tipoPeca.getNome().equals(request.nome()) &&
                tipoPecaRepository.existsByNome(request.nome())) {
            throw new RuntimeException("Já existe outro tipo de peça com este nome");
        }

        tipoPeca.setNome(request.nome());
        tipoPeca.setDescricao(request.descricao());
        tipoPeca.setMetadadosCotas(convertDimensoesToJson(request.dimensoes()));

        TipoPeca updated = tipoPecaRepository.save(tipoPeca);
        return toTipoPecaResponse(updated);
    }

    // Método alternativo se precisar atualizar com entidade
    public TipoPecaResponse atualizarTipoPecaEntity(Long id, TipoPeca tipoPecaAtualizado) {
        TipoPeca tipoPeca = tipoPecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de peça não encontrado"));

        if (!tipoPeca.getNome().equals(tipoPecaAtualizado.getNome()) &&
                tipoPecaRepository.existsByNome(tipoPecaAtualizado.getNome())) {
            throw new RuntimeException("Já existe outro tipo de peça com este nome");
        }

        tipoPeca.setNome(tipoPecaAtualizado.getNome());
        tipoPeca.setDescricao(tipoPecaAtualizado.getDescricao());
        tipoPeca.setMetadadosCotas(tipoPecaAtualizado.getMetadadosCotas());

        TipoPeca updated = tipoPecaRepository.save(tipoPeca);
        return toTipoPecaResponse(updated);
    }

    public void excluirTipoPeca(Long id) {
        TipoPeca tipoPeca = tipoPecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de peça não encontrado"));

        if (tipoPecaRepository.isTipoPecaEmUso(id)) {
            throw new RuntimeException("Não é possível excluir tipo de peça em uso por lotes");
        }

        tipoPecaRepository.delete(tipoPeca);
    }

    public List<Object[]> obterEstatisticasUso() {
        return tipoPecaRepository.countLotesPorTipoPeca();
    }

    // MÉTODO DE CONVERSÃO CORRIGIDO
    private TipoPecaResponse toTipoPecaResponse(TipoPeca tipoPeca) {
        return new TipoPecaResponse(
                tipoPeca.getId(),
                tipoPeca.getNome(),
                tipoPeca.getDescricao(),
                parseMetadadosCotas(tipoPeca.getMetadadosCotas())
        );
    }

    // IMPLEMENTAÇÃO COMPLETA DO PARSE
    private List<CotaMetadata> parseMetadadosCotas(String metadadosJson) {
        if (metadadosJson == null || metadadosJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Tentar parse direto do JSON
            Map<String, Object> metadadosMap = objectMapper.readValue(metadadosJson, Map.class);
            List<Map<String, Object>> dimensoesMap = (List<Map<String, Object>>) metadadosMap.get("dimensoes");

            if (dimensoesMap == null) {
                return Collections.emptyList();
            }

            return dimensoesMap.stream()
                    .map(this::mapToCotaMetadata)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            System.err.println("Erro ao parsear metadados: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private CotaMetadata mapToCotaMetadata(Map<String, Object> cotaMap) {
        try {
            return new CotaMetadata(
                    (String) cotaMap.get("nome"),
                    (String) cotaMap.get("label"),
                    (String) cotaMap.get("tipo"),
                    (String) cotaMap.get("unidade"),
                    cotaMap.get("tolerancia") != null ? ((Number) cotaMap.get("tolerancia")).doubleValue() : null,
                    cotaMap.get("valorPadrao") != null ? ((Number) cotaMap.get("valorPadrao")).doubleValue() : null
            );
        } catch (Exception e) {
            System.err.println("Erro ao mapear cota: " + e.getMessage());
            return null;
        }
    }

    // REMOVA estes métodos duplicados e incorretos:
    // public TipoPecaResponse criarTipoPecaFromRequest(CriarTipoPecaRequest request) {
    //     // Já existe o método criarTipoPeca que faz isso
    // }
    //
    // public TipoPecaResponse atualizarTipoPeca(Long id, CriarTipoPecaRequest request) {
    //     // Já foi implementado acima corretamente
    // }
    //
    // public TipoPecaResponse atualizarTipoPeca(Long id, TipoPeca tipoPecaAtualizado) {
    //     // Conflito de assinatura - use o método atualizarTipoPecaEntity
    // }

    public List<TipoPecaResponse> listarTodosOrdenados() {
        return tipoPecaRepository.findAllOrderByNome().stream()
                .map(this::toTipoPecaResponse)
                .toList();
    }

    // MÉTODO UTilitário para obter template de medição
    public Map<String, Object> getTemplateMedicao(Long tipoPecaId) {
        TipoPeca tipoPeca = tipoPecaRepository.findById(tipoPecaId)
                .orElseThrow(() -> new RuntimeException("Tipo de peça não encontrado"));

        List<CotaMetadata> cotas = parseMetadadosCotas(tipoPeca.getMetadadosCotas());

        Map<String, Object> template = new HashMap<>();
        template.put("tipoPecaId", tipoPeca.getId());
        template.put("tipoPecaNome", tipoPeca.getNome());
        template.put("dimensoesObrigatorias", cotas);

        return template;
    }
}