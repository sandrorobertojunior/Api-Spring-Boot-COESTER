package com.server.coester.services;

import com.server.coester.dtos.DashboardResponse;
import com.server.coester.dtos.LoteResumidoResponse;
import com.server.coester.entities.Lote;
import com.server.coester.repositories.LoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class DashboardService {

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private LoteService loteService;

    public DashboardResponse getDashboard() {
        Object[] estatisticas = loteRepository.getDashboardEstatisticas();
        List<Lote> lotesRecentes = loteRepository.findLotesRecentes();

        return new DashboardResponse(
                ((Number) estatisticas[0]).longValue(),     // totalLotes
                ((Number) estatisticas[1]).longValue(),     // lotesEmAndamento
                ((Number) estatisticas[2]).longValue(),     // lotesConcluidos
                (Double) estatisticas[3],                   // taxaAprovacaoMedia
                lotesRecentes.stream()
                        .map(loteService::toLoteResumidoResponse)
                        .toList()
        );
    }

    // Método alternativo se quiser manter tudo no DashboardService
    private LoteResumidoResponse toLoteResumidoResponse(Lote lote) {
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
}