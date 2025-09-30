package com.server.coester.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipos_peca")
public class TipoPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false)
    private String nome; // "Parafuso M8", "Engrenagem 80mm"

    @Column(length = 500)
    private String descricao;

    // JSON com as cotas/dimensões esperadas (para gerar o formulário no front)
    @Column(name = "metadados_cotas", columnDefinition = "TEXT")
    private String metadadosCotas;

    // Exemplo do JSON que vai armazenar:
    /*
    {
      "dimensoes": [
        {
          "nome": "comprimento",
          "label": "Comprimento (mm)",
          "tipo": "number",
          "unidade": "mm",
          "tolerancia": 0.1,
          "valorPadrao": 50.0
        },
        {
          "nome": "diametro",
          "label": "Diâmetro (mm)",
          "tipo": "number",
          "unidade": "mm",
          "tolerancia": 0.05,
          "valorPadrao": 25.0
        }
      ]
    }
    */

    public TipoPeca() {}
}