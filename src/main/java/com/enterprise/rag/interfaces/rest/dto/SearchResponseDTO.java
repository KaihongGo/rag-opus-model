package com.enterprise.rag.interfaces.rest.dto;

import com.enterprise.rag.domain.model.search.FusionStrategyType;
import com.enterprise.rag.domain.model.search.RetrievalMethodType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 搜索响应 DTO
 */
@Data
@Builder
public class SearchResponseDTO {
    
    private String knowledgeBaseId;
    private List<SearchResultItemDTO> results;
    private Long totalHits;
    private Long took;
    private FusionStrategyType fusionStrategy;
    private Map<String, Object> debugInfo;
    
    /**
     * 搜索结果项 DTO
     */
    @Data
    @Builder
    public static class SearchResultItemDTO {
        private String documentId;
        private Double score;
        private Map<RetrievalMethodType, Double> methodScores;
        private Map<RetrievalMethodType, Integer> methodRanks;
        private Map<String, Object> content;
        private Map<String, List<String>> highlights;
        private Map<String, Object> metadata;
    }
}
