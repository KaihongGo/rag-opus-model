package com.enterprise.rag.infrastructure.persistence.converter;

import com.enterprise.rag.domain.model.knowledgebase.*;
import com.enterprise.rag.infrastructure.persistence.entity.KnowledgeBasePO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库实体转换器
 */
@Component
public class KnowledgeBaseConverter {
    
    private final ObjectMapper objectMapper;
    
    public KnowledgeBaseConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 领域模型 -> 数据库实体
     */
    public KnowledgeBasePO toEntity(KnowledgeBase domain) {
        KnowledgeBasePO entity = new KnowledgeBasePO();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setTenantId(domain.getTenantId());
        entity.setStatus(domain.getStatus().name());
        entity.setIndexName(domain.getIndexName());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        
        // 序列化字段定义
        try {
            List<Map<String, Object>> fieldDefList = new ArrayList<>();
            for (FieldDefinition fd : domain.getFieldDefinitionList()) {
                Map<String, Object> fieldMap = new java.util.HashMap<>();
                fieldMap.put("fieldName", fd.getFieldName());
                fieldMap.put("indexType", fd.getIndexType().name());
                fieldMap.put("isFilter", fd.isFilter());
                fieldMap.put("required", fd.isRequired());
                fieldMap.put("description", fd.getDescription() != null ? fd.getDescription() : "");
                
                if (fd.getEmbeddingConfig() != null) {
                    Map<String, Object> embeddingMap = new java.util.HashMap<>();
                    embeddingMap.put("modelId", fd.getEmbeddingConfig().getModelId());
                    embeddingMap.put("dimension", fd.getEmbeddingConfig().getDimension());
                    embeddingMap.put("similarity", fd.getEmbeddingConfig().getSimilarity());
                    fieldMap.put("embeddingConfig", embeddingMap);
                } else {
                    fieldMap.put("embeddingConfig", null);
                }
                
                fieldDefList.add(fieldMap);
            }
            entity.setFieldDefinitions(objectMapper.writeValueAsString(fieldDefList));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize field definitions", e);
        }
        
        return entity;
    }
    
    /**
     * 数据库实体 -> 领域模型
     */
    public KnowledgeBase toDomain(KnowledgeBasePO entity) {
        List<FieldDefinition> fieldDefinitions = new ArrayList<>();
        
        // 反序列化字段定义
        try {
            if (entity.getFieldDefinitions() != null && !entity.getFieldDefinitions().isBlank()) {
                List<Map<String, Object>> fieldDefList = objectMapper.readValue(
                        entity.getFieldDefinitions(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> fieldMap : fieldDefList) {
                    EmbeddingConfig embeddingConfig = null;
                    Object embConfigObj = fieldMap.get("embeddingConfig");
                    if (embConfigObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> embConfigMap = (Map<String, Object>) embConfigObj;
                        embeddingConfig = EmbeddingConfig.builder()
                                .modelId((String) embConfigMap.get("modelId"))
                                .dimension((Integer) embConfigMap.get("dimension"))
                                .similarity((String) embConfigMap.get("similarity"))
                                .build();
                    }
                    
                    FieldDefinition fd = FieldDefinition.builder()
                            .fieldName((String) fieldMap.get("fieldName"))
                            .indexType(IndexType.valueOf((String) fieldMap.get("indexType")))
                            .isFilter((Boolean) fieldMap.getOrDefault("isFilter", false))
                            .required((Boolean) fieldMap.getOrDefault("required", false))
                            .description((String) fieldMap.get("description"))
                            .embeddingConfig(embeddingConfig)
                            .build();
                    fieldDefinitions.add(fd);
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize field definitions", e);
        }
        
        return KnowledgeBase.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .tenantId(entity.getTenantId())
                .status(KnowledgeBaseStatus.valueOf(entity.getStatus()))
                .fieldDefinitions(fieldDefinitions)
                .indexName(entity.getIndexName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}
