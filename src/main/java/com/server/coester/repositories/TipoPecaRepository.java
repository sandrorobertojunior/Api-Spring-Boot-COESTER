package com.server.coester.repositories;

import com.server.coester.entities.TipoPeca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TipoPecaRepository extends JpaRepository<TipoPeca, Long> {

    Optional<TipoPeca> findByNome(String nome);
    boolean existsByNome(String nome);
    List<TipoPeca> findByNomeContainingIgnoreCase(String nome);

    @Query("SELECT tp FROM TipoPeca tp WHERE tp.id IN " +
            "(SELECT DISTINCT l.tipoPeca.id FROM Lote l)")
    List<TipoPeca> findTiposComLotes();

    @Query("SELECT tp.nome, COUNT(l) FROM TipoPeca tp LEFT JOIN Lote l ON l.tipoPeca = tp GROUP BY tp.nome")
    List<Object[]> countLotesPorTipoPeca();

    @Query("SELECT COUNT(l) > 0 FROM Lote l WHERE l.tipoPeca.id = :tipoPecaId")
    boolean isTipoPecaEmUso(@Param("tipoPecaId") Long tipoPecaId);

    @Query("SELECT tp FROM TipoPeca tp ORDER BY tp.nome ASC")
    List<TipoPeca> findAllOrderByNome();
}