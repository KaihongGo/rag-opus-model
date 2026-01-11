package com.enterprise.rag.interfaces.rest.assembler;

import com.enterprise.rag.domain.model.knowledgebase.EmbeddingConfig;
import com.enterprise.rag.domain.model.knowledgebase.FieldDefinition;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import com.enterprise.rag.domain.model.search.*;
import com.enterprise.rag.interfaces.rest.dto.KnowledgeBaseDTO;
import com.enterprise.rag.interfaces.rest.dto.SearchRequestDTO;
import com.enterprise.rag.interfaces.rest.dto.SearchResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO 与领域模型转换组装器
 */
@Component
public class DtoAssembler {
    
    /**
     * SearchRequestDTO -> SearchRequest (领域模型)
     */
    public SearchRequest toSearchRequest(SearchRequestDTO dto) {
        List<RetrievalMethod> methods = dto.getRetrievalMethods().stream()
                .map(this::toRetrievalMethod)
                .collect(Collectors.toList());
        
        SearchRequest.HighlightConfig highlightConfig = null;
        if (dto.getHighlightConfig() != null) {
            highlightConfig = SearchRequest.HighlightConfig.builder()
                    .fields(dto.getHighlightConfig().getFields())
                    .preTag(dto.getHighlightConfig().getPreTag())
                    .postTag(dto.getHighlightConfig().getPostTag())
                    .fragmentSize(dto.getHighlightConfig().getFragmentSize())
                    .build();
        }
        
        return SearchRequest.builder()
                .knowledgeBaseId(dto.getKnowledgeBaseId())
                .queryText(dto.getQueryText())
                .retrievalMethods(methods)
                .fusionStrategy(dto.getFusionStrategy())
                .rrfK(dto.getRrfK())
                .topK(dto.getTopK())
                .minScore(dto.getMinScore())
                .filters(dto.getFilters())
                .includeVectors(dto.isIncludeVectors())
                .includeMetadata(dto.isIncludeMetadata())
                .highlightConfig(highlightConfig)
                .build();
    }
    
    private RetrievalMethod toRetrievalMethod(SearchRequestDTO.RetrievalMethodDTO dto) {
        RetrievalMethod.VectorSearchConfig vectorConfig = null;
        if (dto.getVectorConfig() != null) {
            vectorConfig = RetrievalMethod.VectorSearchConfig.builder()
                    .vectorField(dto.getVectorConfig().getVectorField())
                    .numCandidates(dto.getVectorConfig().getNumCandidates())
                    .similarity(dto.getVectorConfig().getSimilarity())
                    .build();
        }
        
        RetrievalMethod.TextSearchConfig textConfig = null;
        if (dto.getTextConfig() != null) {
            textConfig = RetrievalMethod.TextSearchConfig.builder()
                    .matchType(dto.getTextConfig().getMatchType())
                    .fieldBoosts(dto.getTextConfig().getFieldBoosts())
                    .fuzziness(dto.getTextConfig().getFuzziness())
                    .analyzer(dto.getTextConfig().getAnalyzer())
                    .build();
        }
        
        return RetrievalMethod.builder()
                .type(dto.getType())
                .weight(dto.getWeight())
                .targetFields(dto.getTargetFields())
                .vectorConfig(vectorConfig)
                .textConfig(textConfig)
                .minScore(dto.getMinScore())
                .build();
    }
    
    /**
     * SearchResult -> SearchResponseDTO
     */
    public SearchResponseDTO toSearchResponseDTO(SearchResult result) {
        List<SearchResponseDTO.SearchResultItemDTO> items = result.getItems().stream()
                .map(this::toSearchResultItemDTO)
                .collect(Collectors.toList());
        
        return SearchResponseDTO.builder()
                .knowledgeBaseId(result.getKnowledgeBaseId())
                .results(items)
                .totalHits(result.getTotalHits())
                .took(result.getTook())
                .fusionStrategy(result.getFusionStrategy())
                .debugInfo(result.getDebugInfo())
                .build();
    }
    
    private SearchResponseDTO.SearchResultItemDTO toSearchResultItemDTO(SearchResultItem item) {
        return SearchResponseDTO.SearchResultItemDTO.builder()
                .documentId(item.getDocumentId())
                .score(item.getScore())
                .methodScores(item.getMethodScores())
                .methodRanks(item.getMethodRanks())
                .content(item.getContent())
                .highlights(item.getHighlights())
                .metadata(item.getMetadata())
                .build();
    }
    
    /**
     * KnowledgeBaseDTO -> KnowledgeBase (领域模型)
     */
    public KnowledgeBase toKnowledgeBase(KnowledgeBaseDTO dto) {
        List<FieldDefinition> fieldDefinitions = dto.getFieldDefinitions().stream()
                .map(this::toFieldDefinition)
                .collect(Collectors.toList());
        
        return KnowledgeBase.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .tenantId(dto.getTenantId())
                .status(dto.getStatus())
                .fieldDefinitions(fieldDefinitions)
                .indexName(dto.getIndexName())
                .build();
    }
    
    private FieldDefinition toFieldDefinition(KnowledgeBaseDTO.FieldDefinitionDTO dto) {
        EmbeddingConfig embeddingConfig = null;
        if (dto.getEmbeddingConfig() != null) {
            embeddingConfig = EmbeddingConfig.builder()
                    .modelId(dto.getEmbeddingConfig().getModelId())
                    .dimension(dto.getEmbeddingConfig().getDimension())
                    .similarity(dto.getEmbeddingConfig().getSimilarity())
                    .build();
        }
        
        return FieldDefinition.builder()
                .fieldName(dto.getFieldName())
                .indexType(dto.getIndexType())
                .isFilter(dto.isFilter())
                .required(dto.isRequired())
                .description(dto.getDescription())
                .embeddingConfig(embeddingConfig)
                .build();
    }
    
    /**
     * KnowledgeBase -> KnowledgeBaseDTO
     */
    public KnowledgeBaseDTO toKnowledgeBaseDTO(KnowledgeBase kb) {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(kb.getId());
        dto.setName(kb.getName());
        dto.setDescription(kb.getDescription());
        dto.setTenantId(kb.getTenantId());
        dto.setStatus(kb.getStatus());
        dto.setIndexName(kb.getIndexName());
        
        List<KnowledgeBaseDTO.FieldDefinitionDTO> fieldDefs = kb.getFieldDefinitionList().stream()
                .map(fd -> {
                    KnowledgeBaseDTO.FieldDefinitionDTO fdDto = new KnowledgeBaseDTO.FieldDefinitionDTO();
                    fdDto.setFieldName(fd.getFieldName());
                    fdDto.setIndexType(fd.getIndexType());
                    fdDto.setFilter(fd.isFilter());
                    fdDto.setRequired(fd.isRequired());
                    fdDto.setDescription(fd.getDescription());
                    
                    if (fd.getEmbeddingConfig() != null) {
                        KnowledgeBaseDTO.EmbeddingConfigDTO embDto = new KnowledgeBaseDTO.EmbeddingConfigDTO();
                        embDto.setModelId(fd.getEmbeddingConfig().getModelId());
                        embDto.setDimension(fd.getEmbeddingConfig().getDimension());
                        embDto.setSimilarity(fd.getEmbeddingConfig().getSimilarity());
                        fdDto.setEmbeddingConfig(embDto);
                    }
                    
                    return fdDto;
                })
                .collect(Collectors.toList());
        
        dto.setFieldDefinitions(fieldDefs);
        
        return dto;
    }
}
