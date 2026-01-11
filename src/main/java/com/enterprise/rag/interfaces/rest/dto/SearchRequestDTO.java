package com.enterprise.rag.interfaces.rest.dto;

import com.enterprise.rag.domain.model.search.FusionStrategyType;
import com.enterprise.rag.domain.model.search.RetrievalMethodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 搜索请求 DTO
 */
@Data
public class SearchRequestDTO {
    
    @NotBlank(message = "Knowledge base ID is required")
    private String knowledgeBaseId;
    
    @NotBlank(message = "Query text is required")
    private String queryText;
    
    @NotNull(message = "At least one retrieval method is required")
    @Valid
    private List<RetrievalMethodDTO> retrievalMethods;
    
    private FusionStrategyType fusionStrategy = FusionStrategyType.RRF;
    
    @Min(value = 1, message = "RRF k must be at least 1")
    @Max(value = 1000, message = "RRF k must not exceed 1000")
    private Integer rrfK = 60;
    
    @Min(value = 1, message = "TopK must be at least 1")
    @Max(value = 100, message = "TopK must not exceed 100")
    private Integer topK = 10;
    
    private Float minScore;
    
    private Map<String, Object> filters;
    
    private boolean includeVectors = false;
    
    private boolean includeMetadata = true;
    
    private HighlightConfigDTO highlightConfig;
    
    /**
     * 检索方法 DTO
     */
    @Data
    public static class RetrievalMethodDTO {
        
        @NotNull(message = "Retrieval method type is required")
        private RetrievalMethodType type;
        
        @Min(value = 0, message = "Weight must be non-negative")
        @Max(value = 10, message = "Weight must not exceed 10")
        private Double weight = 1.0;
        
        private List<String> targetFields;
        
        private VectorSearchConfigDTO vectorConfig;
        
        private TextSearchConfigDTO textConfig;
        
        private Float minScore;
    }
    
    /**
     * 向量检索配置 DTO
     */
    @Data
    public static class VectorSearchConfigDTO {
        private String vectorField;
        private Integer numCandidates = 100;
        private Float similarity = 0.7f;
    }
    
    /**
     * 文本检索配置 DTO
     */
    @Data
    public static class TextSearchConfigDTO {
        private String matchType = "best_fields";
        private Map<String, Float> fieldBoosts;
        private String fuzziness = "AUTO";
        private String analyzer;
    }
    
    /**
     * 高亮配置 DTO
     */
    @Data
    public static class HighlightConfigDTO {
        private List<String> fields;
        private String preTag = "<em>";
        private String postTag = "</em>";
        private Integer fragmentSize = 150;
    }
}
