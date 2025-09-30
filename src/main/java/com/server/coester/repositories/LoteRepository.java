package com.server.coester.repositories;

import com.server.coester.entities.Lote;
import com.server.coester.entities.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    // Buscar por código do lote
    Optional<Lote> findByCodigoLote(String codigoLote);

    // Verificar se código do lote existe
    boolean existsByCodigoLote(String codigoLote);

    List<Lote> findByUsuarioOrderByDataCriacaoDesc(Usuario usuario);
    // Buscar lotes por status
    List<Lote> findByStatus(String status);

    // Buscar lotes por usuário
    List<Lote> findByUsuario(Usuario usuario);

    // Buscar lotes por tipo de peça
    List<Lote> findByTipoPecaId(Long tipoPecaId);

    // Buscar lotes criados entre datas
    List<Lote> findByDataCriacaoBetween(LocalDateTime inicio, LocalDateTime fim);

    // Buscar lotes com status "EM_ANDAMENTO" ordenados por data
    List<Lote> findByStatusOrderByDataCriacaoDesc(String status);

    // Buscar últimos N lotes criados
    List<Lote> findTop5ByOrderByDataCriacaoDesc();

    // Buscar lotes com taxa de aprovação acima de um valor
    @Query("SELECT l FROM Lote l WHERE l.taxaAprovacao >= :taxaMinima")
    List<Lote> findByTaxaAprovacaoGreaterThanEqual(@Param("taxaMinima") Double taxaMinima);

    // Buscar lotes com porcentagem de amostragem específica
    @Query("SELECT l FROM Lote l WHERE l.porcentagemAmostragem BETWEEN :min AND :max")
    List<Lote> findByPorcentagemAmostragemBetween(@Param("min") Double min, @Param("max") Double max);

    // Estatísticas gerais dos lotes
    @Query("SELECT COUNT(l), AVG(l.taxaAprovacao), SUM(l.quantidadePecas) FROM Lote l WHERE l.status = 'CONCLUIDO'")
    Object[] getEstatisticasGerais();

    // Buscar lotes que contenham texto na descrição (case insensitive)
    @Query("SELECT l FROM Lote l WHERE LOWER(l.descricao) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Lote> findByDescricaoContainingIgnoreCase(@Param("texto") String texto);


    @Query(value = """
        SELECT
            COUNT(l.id),
            COALESCE(SUM(CASE WHEN l.status = 'EM_ANDAMENTO' THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN l.status = 'CONCLUIDO' THEN 1L ELSE 0L END), 0L),
            COALESCE(AVG(CASE WHEN l.status = 'CONCLUIDO' THEN l.taxaAprovacao ELSE NULL END), 0.0)
        FROM Lote l
        WHERE l.usuario = :usuario
        """) // Remova a linha "GROUP BY l.usuario"
    List<Object[]> getDashboardEstatisticasByUsuario(@Param("usuario") Usuario usuario);

    // 3. Lotes recentes por usuário
    List<Lote> findLotesRecentesByUsuario(Usuario usuario);

}

// Repository customizado para consultas com JSON
interface CustomLoteRepository {

    // Buscar lotes onde alguma medição tenha observação específica
    List<Lote> findLotesComMedicaoObservacao(String observacao);

    // Buscar lotes com medições reprovadas
    List<Lote> findLotesComMedicoesReprovadas();

    // Calcular estatísticas detalhadas das medições de um lote
    Map<String, Object> getEstatisticasDetalhadasMedicoes(Long loteId);
}

// Implementação do repository customizado
@Repository
class CustomLoteRepositoryImpl implements CustomLoteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Lote> findLotesComMedicaoObservacao(String observacao) {
        String queryStr = "SELECT l.* FROM lotes l " +
                "WHERE l.medicoes_json::text LIKE '%' || :observacao || '%'";

        return entityManager.createNativeQuery(queryStr, Lote.class)
                .setParameter("observacao", observacao)
                .getResultList();
    }

    @Override
    public List<Lote> findLotesComMedicoesReprovadas() {
        String queryStr = "SELECT l.* FROM lotes l " +
                "WHERE l.medicoes_json::text LIKE '%\"status\":\"REPROVADO\"%'";

        return entityManager.createNativeQuery(queryStr, Lote.class)
                .getResultList();
    }

    @Override
    public Map<String, Object> getEstatisticasDetalhadasMedicoes(Long loteId) {
        // Consulta complexa para analisar o JSON de medições
        String queryStr = "SELECT " +
                "jsonb_array_length(medicoes_json) as total_medicoes, " +
                "COUNT(*) FILTER (WHERE medicoes_json @> '[{\"status\":\"APROVADO\"}]') as aprovadas, " +
                "COUNT(*) FILTER (WHERE medicoes_json @> '[{\"status\":\"REPROVADO\"}]') as reprovadas " +
                "FROM lotes WHERE id = :loteId";

        Object[] result = (Object[]) entityManager.createNativeQuery(queryStr)
                .setParameter("loteId", loteId)
                .getSingleResult();

        Map<String, Object> estatisticas = new HashMap<>();
        estatisticas.put("totalMedicoes", result[0]);
        estatisticas.put("aprovadas", result[1]);
        estatisticas.put("reprovadas", result[2]);

        return estatisticas;
    }
}