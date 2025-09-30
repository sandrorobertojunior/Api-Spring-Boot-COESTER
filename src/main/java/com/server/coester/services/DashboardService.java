package com.server.coester.services;

import com.server.coester.dtos.DashboardResponse;
import com.server.coester.dtos.LoteResumidoResponse;
import com.server.coester.dtos.TipoPecaResponse;
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


    private LoteResumidoResponse toLoteResumidoResponse(Lote lote) {
        // Primeiro, criamos o DTO do TipoPeca, como fizemos anteriormente
        TipoPecaResponse tipoPecaDto = new TipoPecaResponse(lote.getTipoPeca());

        return new LoteResumidoResponse(
                lote.getId(),                         // 1. id
                lote.getCodigoLote(),                 // 2. codigoLote
                lote.getDescricao(),                  // 3. descricao
                tipoPecaDto,                          // 4. tipoPeca (CORRIGIDO: agora é um objeto)
                lote.getQuantidadePecas(),            // 5. quantidadePecas
                lote.getQuantidadeAmostras(),         // 6. quantidadeAmostrasDesejada
                lote.getPorcentagemAmostragem(),      // 7. porcentagemAmostragem (ADICIONADO)
                lote.getPecasAprovadas(),             // 8. pecasAprovadas (ADICIONADO)
                lote.getPecasReprovadas(),            // 9. pecasReprovadas (ADICIONADO)
                lote.getTaxaAprovacao(),              // 10. taxaAprovacao (POSIÇÃO CORRIGIDA)
                lote.getStatus().toString(),          // 11. status (POSIÇÃO CORRIGIDA, use .toString() se for um Enum)
                lote.getDataCriacao()                 // 12. dataCriacao (POSIÇÃO CORRIGIDA)
        );
    }
}