package com.enterprise.rag.interfaces.rest.dto;

import com.enterprise.rag.domain.model.knowledgebase.IndexType;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBaseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 知识库请求 DTO
 */
@Data
public class KnowledgeBaseDTO {
    
    private String id;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    private KnowledgeBaseStatus status;
    
    @NotNull(message = "Field definitions are required")
    @Valid
    private List<FieldDefinitionDTO> fieldDefinitions;
    
    private String indexName;
    
    /**
     * 字段定义 DTO
     */
    @Data
    public static class FieldDefinitionDTO {
        
        @NotBlank(message = "Field name is required")
        private String fieldName;
        
        @NotNull(message = "Index type is required")
        private IndexType indexType;
        
        private boolean isFilter = false;
        
        private boolean required = false;
        
        private String description;
        
        private EmbeddingConfigDTO embeddingConfig;
    }
    
    /**
     * 嵌入配置 DTO
     */
    @Data
    public static class EmbeddingConfigDTO {
        private String modelId = "text-embedding-ada-002";
        private Integer dimension = 1536;
        private String similarity = "cosine";
    }
}
