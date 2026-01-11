package com.enterprise.rag.domain.model.knowledgebase;

import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

/**
 * 字段定义 - 值对象
 * 
 * <p>定义知识库中单个字段的元数据配置，包括索引类型、过滤能力和向量化配置</p>
 */
@Getter
@Builder
public class FieldDefinition {
    
    /**
     * 字段名称
     */
    private final String fieldName;
    
    /**
     * 索引类型: VECTOR, TEXT, KEYWORD
     */
    private final IndexType indexType;
    
    /**
     * 是否允许作为检索过滤条件
     */
    private final boolean isFilter;
    
    /**
     * 嵌入向量配置 (仅 VECTOR 类型需要)
     */
    private final EmbeddingConfig embeddingConfig;
    
    /**
     * 字段描述
     */
    private final String description;
    
    /**
     * 是否必填
     */
    private final boolean required;
    
    /**
     * 判断是否为向量字段
     */
    public boolean isVectorField() {
        return IndexType.VECTOR.equals(this.indexType);
    }
    
    /**
     * 判断是否为文本字段
     */
    public boolean isTextField() {
        return IndexType.TEXT.equals(this.indexType);
    }
    
    /**
     * 判断是否为关键词字段
     */
    public boolean isKeywordField() {
        return IndexType.KEYWORD.equals(this.indexType);
    }
    
    /**
     * 获取向量字段名称（存储实际向量的字段）
     */
    public String getVectorFieldName() {
        if (!isVectorField()) {
            throw new IllegalStateException("Only vector fields have vector field names");
        }
        return fieldName + "_vector";
    }
    
    /**
     * 校验字段定义的有效性
     */
    public void validate() {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be null or blank");
        }
        if (indexType == null) {
            throw new IllegalArgumentException("Index type cannot be null");
        }
        if (isVectorField() && embeddingConfig == null) {
            throw new IllegalArgumentException("Vector fields must have embedding config");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldDefinition that = (FieldDefinition) o;
        return Objects.equals(fieldName, that.fieldName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fieldName);
    }
    
    /**
     * 创建向量字段定义
     */
    public static FieldDefinition vectorField(String fieldName, EmbeddingConfig config) {
        return FieldDefinition.builder()
                .fieldName(fieldName)
                .indexType(IndexType.VECTOR)
                .embeddingConfig(config)
                .isFilter(false)
                .required(true)
                .build();
    }
    
    /**
     * 创建文本字段定义
     */
    public static FieldDefinition textField(String fieldName, boolean isFilter) {
        return FieldDefinition.builder()
                .fieldName(fieldName)
                .indexType(IndexType.TEXT)
                .isFilter(isFilter)
                .required(false)
                .build();
    }
    
    /**
     * 创建关键词字段定义
     */
    public static FieldDefinition keywordField(String fieldName, boolean isFilter) {
        return FieldDefinition.builder()
                .fieldName(fieldName)
                .indexType(IndexType.KEYWORD)
                .isFilter(isFilter)
                .required(false)
                .build();
    }
}
