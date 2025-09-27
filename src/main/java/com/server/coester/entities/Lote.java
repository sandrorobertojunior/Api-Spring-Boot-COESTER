package com.server.coester.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lotes")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigoLote;

    @Column(nullable = false)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_peca_id", nullable = false)
    private TipoPeca tipoPeca;

    @Column(nullable = false)
    private Integer quantidadePecas; // Total de peças no lote

    // CAMPO NOVO: Quantidade de amostras planejada (meta)
    @Column(name = "quantidade_amostras_desejada", nullable = false)
    private Integer quantidadeAmostrasDesejada;

    @Column(nullable = false)
    private Integer quantidadeAmostras; // Quantidade REAL de peças medidas

    @Column(nullable = false)
    private Double porcentagemAmostragem;

    @Column(nullable = false)
    private Integer pecasAprovadas;

    @Column(nullable = false)
    private Integer pecasReprovadas;

    @Column(nullable = false)
    private Double taxaAprovacao;

    @Column(name = "medicoes_json", columnDefinition = "TEXT")
    private String medicoesJson;

    @Column(nullable = false)
    private String status; // "EM_ANDAMENTO", "EM_ANALISE", "CONCLUIDO", "APROVADO", "REPROVADO"

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataConclusao;

    @Column(length = 1000)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;

    public Lote() {
        this.dataCriacao = LocalDateTime.now();
        this.status = "EM_ANDAMENTO";
        this.pecasAprovadas = 0;
        this.pecasReprovadas = 0;
        this.taxaAprovacao = 0.0;
        this.quantidadeAmostras = 0;
        this.porcentagemAmostragem = 0.0;
        this.medicoesJson = "[]";
    }

    // REMOVA estes métodos - a lógica fica no Service
    // public void adicionarMedicao(String medicaoJson) {}
    // public void calcularEstatisticas() {}
}