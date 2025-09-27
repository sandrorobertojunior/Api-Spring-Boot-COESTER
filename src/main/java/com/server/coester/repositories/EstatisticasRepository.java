package com.server.coester.repositories;

import com.server.coester.entities.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface EstatisticasRepository extends JpaRepository<Lote, Long> {

    // Estatísticas por período
    @Query("SELECT " +
            "FUNCTION('DATE', l.dataCriacao) as data, " +
            "COUNT(l) as totalLotes, " +
            "AVG(l.taxaAprovacao) as taxaMedia " +
            "FROM Lote l " +
            "WHERE l.dataCriacao BETWEEN :inicio AND :fim " +
            "GROUP BY FUNCTION('DATE', l.dataCriacao) " +
            "ORDER BY data")
    List<Object[]> getEstatisticasPorPeriodo(@Param("inicio") LocalDateTime inicio,
                                             @Param("fim") LocalDateTime fim);

    // Top tipos de peça mais utilizados
    @Query("SELECT tp.nome, COUNT(l) as total " +
            "FROM Lote l JOIN l.tipoPeca tp " +
            "GROUP BY tp.nome " +
            "ORDER BY total DESC")
    List<Object[]> getTiposPecaMaisUtilizados();

    // Performance por usuário
    @Query("SELECT u.username, COUNT(l) as totalLotes, AVG(l.taxaAprovacao) as taxaMedia " +
            "FROM Lote l JOIN l.usuario u " +
            "GROUP BY u.username " +
            "ORDER BY taxaMedia DESC")
    List<Object[]> getPerformancePorUsuario();
}