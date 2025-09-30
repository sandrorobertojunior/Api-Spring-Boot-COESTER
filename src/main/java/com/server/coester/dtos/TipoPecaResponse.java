package com.server.coester.dtos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.coester.entities.TipoPeca;
import jakarta.persistence.Column;

import java.util.Collections;
import java.util.List;

public record TipoPecaResponse(
        Long id,
        String nome,
        String descricao,
        List<CotaMetadata> metadadosCotas
) {

    public TipoPecaResponse(TipoPeca tipoPeca) {
        this(
                tipoPeca.getId(),
                tipoPeca.getNome(),
                tipoPeca.getDescricao(),
                // Extrai a lista de dentro do JSON {"dimensoes": [...]}
                parseMetadadosCotas(tipoPeca.getMetadadosCotas())
        );
    }

    /**
     * Método auxiliar para desserializar a string JSON que está no formato {"dimensoes": [...]}.
     * @param json A string JSON do banco de dados.
     * @return Uma lista de CotaMetadata.
     */
    private static List<CotaMetadata> parseMetadadosCotas(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            // 1. Lê a string JSON em uma árvore de nós (JsonNode)
            JsonNode rootNode = mapper.readTree(json);

            // 2. Navega até o nó "dimensoes" dentro do objeto JSON
            JsonNode dimensoesNode = rootNode.get("dimensoes");

            // 3. Verifica se o nó existe e se é um array antes de converter
            if (dimensoesNode != null && dimensoesNode.isArray()) {
                // 4. Converte o nó do array (e não o JSON inteiro) para a lista de objetos
                return mapper.convertValue(dimensoesNode, new TypeReference<List<CotaMetadata>>() {});
            }

            // Retorna lista vazia se a chave "dimensoes" não for encontrada ou não for um array
            return Collections.emptyList();

        } catch (JsonProcessingException e) {
            // Lança uma exceção se o JSON estiver malformado
            throw new RuntimeException("Falha ao processar o JSON de metadados_cotas", e);
        }
    }
}
