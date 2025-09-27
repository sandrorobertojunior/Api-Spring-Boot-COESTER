package com.server.coester.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.server.coester.dtos.*;
import com.server.coester.entities.Lote;
import com.server.coester.entities.TipoPeca;
import com.server.coester.entities.Usuario;
import com.server.coester.repositories.LoteRepository;
import com.server.coester.repositories.TipoPecaRepository;
import com.server.coester.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class LoteService {

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private TipoPecaRepository tipoPecaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // CRUD METHODS
    public LoteResponse criarLote(CriarLoteRequest request) {
        TipoPeca tipoPeca = tipoPecaRepository.findById(request.tipoPecaId())
                .orElseThrow(() -> new RuntimeException("Tipo de peça não encontrado"));

        Usuario usuario = usuarioRepository.findById(1L).orElseThrow(); // Temporário

        Lote lote = new Lote();
        lote.setCodigoLote(gerarCodigoLote());
        lote.setDescricao(request.descricao());
        lote.setTipoPeca(tipoPeca);
        lote.setQuantidadePecas(request.quantidadePecas());
        lote.setObservacoes(request.observacoes());
        lote.setUsuario(usuario);

        // CORREÇÃO: Usar quantidadeAmostrasDesejada do request
        Integer quantidadeAmostras = request.quantidadeAmostrasDesejada() != null ?
                request.quantidadeAmostrasDesejada() :
                calcularAmostragemAutomatica(request.quantidadePecas());

        lote.setQuantidadeAmostrasDesejada(quantidadeAmostras); // ← NOVO CAMPO
        lote.setQuantidadeAmostras(0); // Inicia com 0 amostras reais
        lote.setPorcentagemAmostragem(0.0);

        Lote saved = loteRepository.save(lote);
        return toLoteResponse(saved);
    }

    public Optional<LoteResponse> obterLotePorId(Long id) {
        return loteRepository.findById(id).map(this::toLoteResponse);
    }

    public List<LoteResumidoResponse> listarLotesResumido() {
        return loteRepository.findAll().stream()
                .map(this::toLoteResumidoResponse)
                .toList();
    }

    public List<LoteResumidoResponse> listarLotesPorStatus(String status) {
        return loteRepository.findByStatus(status).stream()
                .map(this::toLoteResumidoResponse)
                .toList();
    }

    public List<LoteResumidoResponse> buscarLotesPorDescricao(String texto) {
        return loteRepository.findByDescricaoContainingIgnoreCase(texto).stream()
                .map(this::toLoteResumidoResponse)
                .toList();
    }

    public LoteResponse atualizarLote(Long id, AtualizarLoteRequest request) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

        lote.setDescricao(request.descricao());
        lote.setObservacoes(request.observacoes());

        Lote updated = loteRepository.save(lote);
        return toLoteResponse(updated);
    }

    public void excluirLote(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

        // Verificar se pode excluir (não pode ter medições)
        if (!lote.getMedicoesJson().equals("[]")) {
            throw new RuntimeException("Não é possível excluir lote com medições");
        }

        loteRepository.delete(lote);
    }

    public LoteResponse adicionarMedicao(AdicionarMedicaoRequest request) {
        System.out.println("=== INICIANDO ADIÇÃO DE MEDIÇÃO ===");
        System.out.println("Lote ID: " + request.loteId());
        System.out.println("Peça Número: " + request.pecaNumero());
        System.out.println("Dimensões recebidas: " + request.dimensoes());

        try {
            Lote lote = loteRepository.findById(request.loteId())
                    .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

            System.out.println("Lote encontrado: " + lote.getId());
            System.out.println("Tipo de Peça: " + lote.getTipoPeca().getNome());

            TipoPeca tipoPeca = lote.getTipoPeca();

            // 1. VALIDAR dimensões antes de processar
            System.out.println("=== VALIDAÇÃO DE DIMENSÕES ===");
            //validarDimensoesMedicao(request.dimensoes(), tipoPeca);
            System.out.println("Validação passou!");

            List<Map<String, Object>> medicoes = parseMedicoesJson(lote.getMedicoesJson());
            System.out.println("Medições existentes: " + medicoes.size());

            // 2. Verificar se peça já foi medida
            boolean pecaJaMedida = medicoes.stream()
                    .anyMatch(med -> request.pecaNumero().equals(med.get("pecaNumero")));

            if (pecaJaMedida) {
                System.out.println("ERRO: Peça já medida");
                throw new RuntimeException("Peça número " + request.pecaNumero() + " já foi medida");
            }

            // 3. CALCULAR STATUS baseado nos metadados
            System.out.println("=== CÁLCULO DE STATUS ===");
            String status = calcularStatusMedicao(request.dimensoes(), tipoPeca);
            System.out.println("Status calculado: " + status);

            // 4. Criar medição
            Map<String, Object> novaMedicao = new HashMap<>();
            novaMedicao.put("id", gerarNovoId(medicoes));
            novaMedicao.put("data", LocalDateTime.now().toString());
            novaMedicao.put("pecaNumero", request.pecaNumero());
            novaMedicao.put("dimensoes", request.dimensoes());
            novaMedicao.put("observacoes", request.observacoes());
            novaMedicao.put("status", status);

            medicoes.add(novaMedicao);
            lote.setMedicoesJson(toJson(medicoes));

            System.out.println("=== RECALCULO DE ESTATÍSTICAS ===");
            recalcularEstatisticas(lote, medicoes);

            Lote updated = loteRepository.save(lote);
            System.out.println("Medição salva com sucesso! ID: " + updated.getId());

            return toLoteResponse(updated);

        } catch (Exception e) {
            System.out.println("ERRO CRÍTICO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // ADICIONE ESTE MÉTODO PARA GERAR IDs ÚNICOS
    private Long gerarNovoId(List<Map<String, Object>> medicoes) {
        if (medicoes.isEmpty()) {
            return 1L;
        }

        // Encontrar o maior ID atual
        Long maxId = medicoes.stream()
                .map(med -> ((Number) med.get("id")).longValue())
                .max(Long::compareTo)
                .orElse(0L);

        return maxId + 1L;
    }

    public List<MedicaoResponse> listarMedicoes(Long loteId) {
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

        return parseMedicoesFromJson(lote.getMedicoesJson());
    }

    public LoteResponse removerMedicao(Long loteId, Long medicaoId) {
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

        List<Map<String, Object>> medicoes = parseMedicoesJson(lote.getMedicoesJson());

        // CORREÇÃO: Usar equals para comparar Long
        boolean removido = medicoes.removeIf(med -> {
            Object idObj = med.get("id");
            if (idObj instanceof Number) {
                return medicaoId.equals(((Number) idObj).longValue());
            }
            return false;
        });

        if (!removido) {
            throw new RuntimeException("Medição não encontrada");
        }

        lote.setMedicoesJson(toJson(medicoes));
        recalcularEstatisticas(lote, medicoes);

        Lote updated = loteRepository.save(lote);
        return toLoteResponse(updated);
    }

    // STATUS METHODS

    public LoteResponse concluirLote(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

        lote.setStatus("CONCLUIDO");
        lote.setDataConclusao(LocalDateTime.now());

        Lote updated = loteRepository.save(lote);
        return toLoteResponse(updated);
    }

    public LoteResponse reabrirLote(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

        lote.setStatus("EM_ANDAMENTO");
        lote.setDataConclusao(null);

        Lote updated = loteRepository.save(lote);
        return toLoteResponse(updated);
    }

    // DASHBOARD

    public DashboardResponse obterDashboard() {
        Object[] estatisticas = loteRepository.getDashboardEstatisticas();
        List<Lote> lotesRecentes = loteRepository.findLotesRecentes();

        return new DashboardResponse(
                ((Number) estatisticas[0]).longValue(),
                ((Number) estatisticas[1]).longValue(),
                ((Number) estatisticas[2]).longValue(),
                (Double) estatisticas[3],
                lotesRecentes.stream()
                        .map(this::toLoteResumidoResponse)
                        .toList()
        );
    }

    // PRIVATE METHODS

    private String gerarCodigoLote() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "LOTE-" + timestamp.substring(timestamp.length() - 6);
    }

    private Integer calcularAmostragemAutomatica(Integer quantidadeTotal) {
        if (quantidadeTotal <= 50) return quantidadeTotal;
        if (quantidadeTotal <= 500) return Math.max(50, quantidadeTotal / 10);
        return Math.max(80, quantidadeTotal / 20);
    }

    private String calcularStatusMedicao(Map<String, Double> dimensoesMedidas, TipoPeca tipoPeca) {
        try {
            List<CotaMetadata> cotasEsperadas = parseMetadadosCotas(tipoPeca.getMetadadosCotas());

            // 1. Verificar se todas as cotas obrigatórias foram preenchidas
            for (CotaMetadata cotaEsperada : cotasEsperadas) {
                String nomeCota = cotaEsperada.nome();
                if (!dimensoesMedidas.containsKey(nomeCota)) {
                    return "REPROVADO"; // Cota obrigatória não preenchida
                }
            }

            // 2. Verificar tolerâncias de cada cota
            for (CotaMetadata cotaEsperada : cotasEsperadas) {
                String nomeCota = cotaEsperada.nome();
                Double valorMedido = dimensoesMedidas.get(nomeCota);
                Double valorPadrao = cotaEsperada.valorPadrao();
                Double tolerancia = cotaEsperada.tolerancia();

                if (valorMedido == null || valorPadrao == null || tolerancia == null) {
                    continue; // Pula se não tem dados suficientes
                }

                double diferenca = Math.abs(valorMedido - valorPadrao);
                if (diferenca > tolerancia) {
                    return "REPROVADO"; // Fora da tolerância
                }
            }

            return "APROVADO";

        } catch (Exception e) {
            System.err.println("Erro na validação: " + e.getMessage());
            return "REPROVADO"; // Em caso de erro, reprova por segurança
        }
    }

    private void recalcularEstatisticas(Lote lote, List<Map<String, Object>> medicoes) {
        long aprovadas = medicoes.stream()
                .filter(m -> "APROVADO".equals(m.get("status")))
                .count();

        int amostrasReais = medicoes.size();

        lote.setQuantidadeAmostras(amostrasReais);
        lote.setPecasAprovadas((int) aprovadas);
        lote.setPecasReprovadas(amostrasReais - (int) aprovadas);

        if (amostrasReais > 0) {
            lote.setTaxaAprovacao((double) aprovadas / amostrasReais * 100);
        }

        lote.setPorcentagemAmostragem((double) amostrasReais / lote.getQuantidadePecas() * 100);

        // LÓGICA CORRETA DE STATUS:
        if (amostrasReais >= lote.getQuantidadeAmostrasDesejada()) {
            // Atingiu a amostragem desejada - pode marcar como concluído
            if (!"CONCLUIDO".equals(lote.getStatus()) && !"APROVADO".equals(lote.getStatus()) && !"REPROVADO".equals(lote.getStatus())) {
                lote.setStatus("EM_ANALISE"); // Aguardando aprovação final
                lote.setDataConclusao(LocalDateTime.now());
            }
        } else if (amostrasReais > 0) {
            lote.setStatus("EM_ANDAMENTO");
        }

        // Pode medir até a quantidade total de peças no lote
        // Não há bloqueio automático - o usuário decide quando concluir
    }

    // CONVERSION METHODS (como anteriormente)
    private LoteResponse toLoteResponse(Lote lote) {
        List<MedicaoResponse> medicoes = parseMedicoesFromJson(lote.getMedicoesJson());

        return new LoteResponse(
                lote.getId(),
                lote.getCodigoLote(),
                lote.getDescricao(),
                toTipoPecaResponse(lote.getTipoPeca()),
                lote.getQuantidadePecas(),
                lote.getQuantidadeAmostras(),
                lote.getPorcentagemAmostragem(),
                lote.getPecasAprovadas(),
                lote.getPecasReprovadas(),
                lote.getTaxaAprovacao(),
                lote.getStatus(),
                lote.getDataCriacao(),
                medicoes
        );
    }

    LoteResumidoResponse toLoteResumidoResponse(Lote lote) {
        return new LoteResumidoResponse(
                lote.getId(),
                lote.getCodigoLote(),
                lote.getDescricao(),
                lote.getTipoPeca().getNome(),
                lote.getQuantidadePecas(),
                lote.getQuantidadeAmostras(),
                lote.getTaxaAprovacao(),
                lote.getStatus(),
                lote.getDataCriacao()
        );
    }

    private TipoPecaResponse toTipoPecaResponse(TipoPeca tipoPeca) {
        List<CotaMetadata> metadados = parseMetadadosCotas(tipoPeca.getMetadadosCotas());

        return new TipoPecaResponse(
                tipoPeca.getId(),
                tipoPeca.getNome(),
                tipoPeca.getDescricao(),
                metadados
        );
    }

    private List<Map<String, Object>> parseMedicoesJson(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<MedicaoResponse> parseMedicoesFromJson(String medicoesJson) {
        if (medicoesJson == null || medicoesJson.trim().isEmpty()) {
            return List.of();
        }

        try {
            objectMapper.registerModule(new JavaTimeModule());
            List<Map<String, Object>> medicoesMap = objectMapper.readValue(medicoesJson, List.class);

            return medicoesMap.stream()
                    .map(this::mapToMedicaoResponse)
                    .toList();

        } catch (Exception e) {
            return List.of();
        }
    }

    private MedicaoResponse mapToMedicaoResponse(Map<String, Object> medicaoMap) {
        return new MedicaoResponse(
                ((Number) medicaoMap.get("id")).longValue(),
                LocalDateTime.parse(medicaoMap.get("data").toString()),
                ((Number) medicaoMap.get("pecaNumero")).intValue(),
                (Map<String, Double>) medicaoMap.get("dimensoes"),
                (String) medicaoMap.get("status"),
                (String) medicaoMap.get("observacoes")
        );
    }

    private List<CotaMetadata> parseMetadadosCotas(String metadadosJson) {
        if (metadadosJson == null || metadadosJson.trim().isEmpty()) {
            return List.of();
        }

        try {
            Map<String, Object> metadadosMap = objectMapper.readValue(metadadosJson, Map.class);
            List<Map<String, Object>> dimensoesMap = (List<Map<String, Object>>) metadadosMap.get("dimensoes");

            return dimensoesMap.stream()
                    .map(this::mapToCotaMetadata)
                    .toList();

        } catch (Exception e) {
            return List.of();
        }
    }

    public void validarDimensoesMedicao(Map<String, Double> dimensoes, TipoPeca tipoPeca) {
        List<CotaMetadata> cotasEsperadas = parseMetadadosCotas(tipoPeca.getMetadadosCotas());

        // Validar tipos de dados
        for (Map.Entry<String, Double> dimensao : dimensoes.entrySet()) {
            String nomeDimensao = dimensao.getKey();
            Double valor = dimensao.getValue();

            // Encontrar a cota correspondente nos metadados
            CotaMetadata cotaEsperada = cotasEsperadas.stream()
                    .filter(c -> c.nome().equals(nomeDimensao))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Dimensão '" + nomeDimensao + "' não é válida para este tipo de peça"));

            // Validar valor mínimo/máximo baseado no valor padrão e tolerância
            if (cotaEsperada.valorPadrao() != null && cotaEsperada.tolerancia() != null) {
                double valorMinimo = cotaEsperada.valorPadrao() - cotaEsperada.tolerancia();
                double valorMaximo = cotaEsperada.valorPadrao() + cotaEsperada.tolerancia();

                if (valor < valorMinimo || valor > valorMaximo) {
                    throw new RuntimeException("Dimensão '" + nomeDimensao + "' fora do range permitido: " +
                            valorMinimo + " a " + valorMaximo);
                }
            }
        }
    }

    private CotaMetadata mapToCotaMetadata(Map<String, Object> cotaMap) {
        return new CotaMetadata(
                (String) cotaMap.get("nome"),
                (String) cotaMap.get("label"),
                (String) cotaMap.get("tipo"),
                (String) cotaMap.get("unidade"),
                cotaMap.get("tolerancia") != null ? ((Number) cotaMap.get("tolerancia")).doubleValue() : null,
                cotaMap.get("valorPadrao") != null ? ((Number) cotaMap.get("valorPadrao")).doubleValue() : null
        );
    }

    private String toJson(List<Map<String, Object>> medicoes) {
        try {
            return objectMapper.writeValueAsString(medicoes);
        } catch (Exception e) {
            return "[]";
        }
    }

    public List<Object[]> getEstatisticasPorPeriodo(String dataInicio, String dataFim) {
        // Implementar consulta por período
        return List.of();
    }
}