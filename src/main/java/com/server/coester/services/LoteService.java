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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoteService {

    private static final Double TAXA_APROVACAO_MINIMA = 90.0;
    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private TipoPecaRepository tipoPecaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsuarioService usuarioService;

    public List<LoteResumidoResponse> listarLotesDoUsuario() {
        // 1. Obtém o usuário logado
        Usuario usuario = usuarioService.getUsuarioAutenticado();

        // 2. Busca os lotes por este usuário (necessário método no LoteRepository)
        // MÉTODO NO REPOSITORY NECESSÁRIO: List<Lote> findByUsuario(Usuario usuario);
        return loteRepository.findByUsuario(usuario).stream()
                .map(this::toLoteResumidoResponse)
                .toList();
    }

    // ⭐ NOVO MÉTODO: Lista todos os lotes (Para uso do Administrador)
    public List<LoteResumidoResponse> listarTodosOsLotes() {
        // 1. Chame o repositório para buscar todos os lotes
        // Se TipoPeca for LAZY (como na sua entidade), você DEVE garantir que ele seja
        // carregado aqui, ou terá um LazyInitializationException.
        // A melhor prática seria ter um método findAllComTipoPeca() no repositório.
        List<Lote> todosLotes = loteRepository.findAll();

        // 2. Mapeie a lista de Lote para List<LoteResumidoResponse>
        return todosLotes.stream()
                .map(this::toLoteResumidoResponse)
                .collect(Collectors.toList());
    }

    // CRUD METHODS
    public LoteResponse criarLote(CriarLoteRequest request) {
        TipoPeca tipoPeca = tipoPecaRepository.findById(request.tipoPecaId())
                .orElseThrow(() -> new RuntimeException("Tipo de peça não encontrado"));

        Usuario usuario = usuarioService.getUsuarioAutenticado();
        System.out.println(request.quantidadeAmostrasDesejada());
        // --- Criação da Entidade ---
        Lote lote = new Lote();
        lote.setCodigoLote(gerarCodigoLote());
        lote.setDescricao(request.descricao());
        lote.setTipoPeca(tipoPeca);
        lote.setQuantidadePecas(request.quantidadePecas());
        lote.setObservacoes(request.observacoes());
        lote.setUsuario(usuario);

        // 1. Define o LIMITE DE AMOSTRAS
        lote.setQuantidadeAmostrasDesejada(request.quantidadeAmostrasDesejada());

        // 2. Define o CONTADOR REAL de amostras (inicia em 0)
        lote.setQuantidadeAmostras(0);

        // 3. Define a Porcentagem de Amostragem (Calculada)
        Double porcentagem = (double) request.quantidadeAmostrasDesejada() / lote.getQuantidadePecas();
        lote.setPorcentagemAmostragem(porcentagem * 100.0); // Multiplicar por 100 para ser porcentagem

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

        // 1. OBTÉM O USUÁRIO LOGADO
        Usuario usuarioLogado = usuarioService.getUsuarioAutenticado();
        boolean ehAdmin = usuarioLogado.getArrayRoles().contains("ADMINISTRADOR");
        boolean ehDono = lote.getUsuario().getId().equals(usuarioLogado.getId());

        // 2. VERIFICAÇÃO DE PERMISSÃO
        if (!ehAdmin && (!ehDono || !"EM_ANDAMENTO".equals(lote.getStatus()))) {
            // Se NÃO for Admin E (NÃO for Dono OU o status não for EM_ANDAMENTO)
            throw new AccessDeniedException("Acesso negado. Apenas administradores ou o criador de lotes 'EM_ANDAMENTO' podem excluir.");
        }

        // 3. VERIFICAÇÃO DE DADOS (Se tem medições)
        // O service deve lidar com a exceção de negócio
        if (!lote.getMedicoesJson().equals("[]")) {
            throw new RuntimeException("Não é possível excluir lote com medições");
        }

        // 4. DELEÇÃO
        loteRepository.delete(lote);
    }

    public LoteResponse adicionarMedicao(AdicionarMedicaoRequest request) {
        System.out.println("=== INICIANDO ADIÇÃO DE MEDIÇÃO ===");
        System.out.println("Lote ID: " + request.loteId());
        // REMOVIDO: System.out.println("Peça Número: " + request.pecaNumero());
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

            // 2. LÓGICA DE GERAÇÃO AUTOMÁTICA DO pecaNumero
            Integer proximoPecaNumero = gerarProximoPecaNumero(medicoes);
            System.out.println("Próxima Peça Número gerada: " + proximoPecaNumero);

            // 3. REMOVER: A verificação de peça já medida não é mais necessária,
            // pois o número gerado é sempre único.
        /*
        boolean pecaJaMedida = medicoes.stream()
                .anyMatch(med -> request.pecaNumero().equals(med.get("pecaNumero")));

        if (pecaJaMedida) {
            System.out.println("ERRO: Peça já medida");
            throw new RuntimeException("Peça número " + request.pecaNumero() + " já foi medida");
        }
        */

            // 4. CALCULAR STATUS baseado nos metadados
            System.out.println("=== CÁLCULO DE STATUS ===");
            String status = calcularStatusMedicao(request.dimensoes(), tipoPeca);
            System.out.println("Status calculado: " + status);

            // 5. Criar medição
            Map<String, Object> novaMedicao = new HashMap<>();
            novaMedicao.put("id", gerarNovoId(medicoes));
            novaMedicao.put("data", LocalDateTime.now().toString());
            novaMedicao.put("pecaNumero", proximoPecaNumero); // USANDO O VALOR GERADO
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


    // LoteService.java (Adicione este método auxiliar)

    private Integer gerarProximoPecaNumero(List<Map<String, Object>> medicoes) {
        if (medicoes.isEmpty()) {
            return 1;
        }

        // Encontra o maior 'pecaNumero' existente
        Integer maxPecaNumero = medicoes.stream()
                // Filtra para garantir que o campo exista e seja Number
                .filter(med -> med.get("pecaNumero") instanceof Number)
                // Converte para Integer
                .map(med -> ((Number) med.get("pecaNumero")).intValue())
                // Encontra o máximo
                .max(Integer::compareTo)
                .orElse(0); // Se houver erro ou a lista for vazia, retorna 0 (para retornar 1 no final)

        return maxPecaNumero + 1;
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
        System.out.println(lote);
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

    public Boolean concluirLote(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

        // 2. VERIFICAÇÃO DE PRÉ-REQUISITOS
        if (lote.getQuantidadeAmostras() < lote.getQuantidadeAmostrasDesejada()) {
            throw new RuntimeException("O lote não atingiu o número mínimo de amostras desejadas ("
                    + lote.getQuantidadeAmostrasDesejada() + ") para conclusão. Amostras atuais: "
                    + lote.getQuantidadeAmostras());
        }

        // A taxa de aprovação já foi calculada no método 'recalcularEstatisticas'
        Double taxaAprovacao = lote.getTaxaAprovacao();

        // 3. AVALIAÇÃO DA APROVAÇÃO
        boolean aprovado = taxaAprovacao >= TAXA_APROVACAO_MINIMA;

        // 4. ATUALIZAÇÃO DO STATUS FINAL E DATA DE CONCLUSÃO
        String statusFinal = aprovado ? "APROVADO" : "REPROVADO";

        lote.setStatus(statusFinal);
        lote.setDataConclusao(LocalDateTime.now());

        loteRepository.save(lote);

        // 5. RETORNA O RESULTADO DA APROVAÇÃO
        return aprovado;
    }
    public LoteResponse reabrirLote(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

        lote.setStatus("EM_ANDAMENTO");
        lote.setDataConclusao(null);

        Lote updated = loteRepository.save(lote);
        return toLoteResponse(updated);
    }

    // DashboardService
    public DashboardResponse obterDashboard() {

        // 1. CHAMA O MÉTODO QUE RETORNA LISTA E GARANTE QUE A LISTA TEM UM ELEMENTO
        List<Object[]> resultados = loteRepository.getDashboardEstatisticasByUsuario(usuarioService.getUsuarioAutenticado());

        // Se a lista estiver vazia (sem lotes para o usuário), use um array vazio para evitar erro.
        Object[] estatisticas;
        if (resultados.isEmpty()) {
            // Retorna um array de zeros para evitar NullPointer
            estatisticas = new Object[] {0L, 0L, 0L, 0.0};
        } else {
            // ESTA LINHA ONDE ESTAVA O PROBLEMA FOI SUBSTITUÍDA PELO .get(0)
            estatisticas = resultados.get(0);
        }

        List<Lote> lotesRecentes = loteRepository.findLotesRecentesByUsuario(usuarioService.getUsuarioAutenticado());

        // 2. TRATAMENTO DOS VALORES
        // Adicionado checagem de NULL para segurança, especialmente no índice [3]
        Long totalLotes = estatisticas[0] != null ? ((Number) estatisticas[0]).longValue() : 0L;
        Long lotesEmAndamento = estatisticas[1] != null ? ((Number) estatisticas[1]).longValue() : 0L;
        Long lotesConcluidos = estatisticas[2] != null ? ((Number) estatisticas[2]).longValue() : 0L;
        Double taxaAprovacaoGeral = estatisticas[3] != null ? ((Number) estatisticas[3]).doubleValue() : 0.0;

        // Como seu DTO anterior esperava 5 argumentos, e a query só tem 4,
        // defina o quinto (tempoMedio) explicitamente.
        Double tempoMedioMedicaoMinutos = 0.0;

        return new DashboardResponse(
                totalLotes,
                lotesEmAndamento,
                lotesConcluidos,
                taxaAprovacaoGeral,
                tempoMedioMedicaoMinutos, // O quinto argumento numérico
                lotesRecentes.stream()
                        .map(this::toLoteResumidoResponse)
                        .toList()
        );
    }

    public LoteResponse recomecarLote(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

        // 1. OBTÉM O USUÁRIO LOGADO
        Usuario usuarioLogado = usuarioService.getUsuarioAutenticado();

        // --- RESET DOS DADOS ---

        // 3. Zera o campo de medições (deixa vazio)
        lote.setMedicoesJson("[]");

        // 4. Zera os contadores
        lote.setQuantidadeAmostras(0);
        lote.setPecasAprovadas(0);
        lote.setPecasReprovadas(0);

        // 5. Zera as estatísticas
        lote.setTaxaAprovacao(0.0);

        // 6. Reseta o status para EM_ANDAMENTO
        lote.setStatus("EM_ANDAMENTO");
        lote.setDataConclusao(null); // Remove data de conclusão, se houver

        // 7. Salva e retorna
        Lote updated = loteRepository.save(lote);
        return toLoteResponse(updated);
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
        } else {
            lote.setTaxaAprovacao(0.0);
        }

        lote.setPorcentagemAmostragem((double) amostrasReais / lote.getQuantidadePecas() * 100);

        // LÓGICA CORRETA DE STATUS:
        // Evita alterar o status se o lote já estiver nos estados finais
        if ("APROVADO".equals(lote.getStatus()) || "REPROVADO".equals(lote.getStatus())) {
            return; // Mantém o status final
        }

        if (amostrasReais >= lote.getQuantidadeAmostrasDesejada()) {
            // Se atingiu a amostragem desejada, marca como pronto para a conclusão manual
            // O status "EM_ANALISE" é renomeado para "PRONTO_PARA_CONCLUSAO" para clareza
            // E removemos o setDataConclusao()
            lote.setStatus("PRONTO_PARA_CONCLUSAO");
        } else if (amostrasReais > 0) {
            lote.setStatus("EM_ANDAMENTO");
        } else {
            lote.setStatus("EM_ANDAMENTO"); // Lote sem medições, mas criado
        }

        // A data de conclusão só é definida no método 'concluirLote'
    }

    // CONVERSION METHODS (CORRIGIDO)
    private LoteResponse toLoteResponse(Lote lote) {
        List<MedicaoResponse> medicoes = parseMedicoesFromJson(lote.getMedicoesJson());

        // Calcula a porcentagem com base no valor DESEJADO (meta)
        Double porcentagem = (double) lote.getQuantidadeAmostrasDesejada() / lote.getQuantidadePecas() * 100.0;

        return new LoteResponse(
                lote.getId(),
                lote.getCodigoLote(),
                lote.getDescricao(),
                toTipoPecaResponse(lote.getTipoPeca()),
                lote.getQuantidadePecas(),

                // 🚨 REPARO CRÍTICO AQUI:
                // Mapeia o campo DESEJADO (META, ex: 5) para o campo 'quantidadeAmostras' do DTO.
                lote.getQuantidadeAmostrasDesejada(), // <--- CORREÇÃO

                porcentagem, // Usa a porcentagem calculada
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
                // O quarto argumento deve ser um objeto TipoPecaResponse.
                // A forma de criá-lo depende da definição de TipoPecaResponse.
                // Exemplo, assumindo que ele tenha um construtor que aceita a entidade TipoPeca:
                new TipoPecaResponse(lote.getTipoPeca()), // Ajuste conforme necessário
                lote.getQuantidadePecas(),
                lote.getQuantidadeAmostrasDesejada(), // Mapeado para 'quantidadeAmostrasDesejada'
                lote.getPorcentagemAmostragem(), // Argumento adicionado
                lote.getPecasAprovadas(), // Argumento adicionado
                lote.getPecasReprovadas(), // Argumento adicionado
                lote.getTaxaAprovacao(), // Posição corrigida
                lote.getStatus(), // Posição corrigida
                lote.getDataCriacao() // Posição corrigida
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