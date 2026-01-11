package com.enterprise.rag.domain.model.knowledgebase;

import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

/**
 * 嵌入向量配置 - 值对象
 * 
 * <p>定义向量字段的嵌入模型配置</p>
 */
@Getter
@Builder
public class EmbeddingConfig {
    
    /**
     * 嵌入模型 ID
     */
    private final String modelId;
    
    /**
     * 向量维度
     */
    private final Integer dimension;
    
    /**
     * 相似度算法 (cosine, dot_product, l2_norm)
     */
    private final String similarity;
    
    /**
     * 创建默认配置
     */
    public static EmbeddingConfig defaultConfig() {
        return EmbeddingConfig.builder()
                .modelId("text-embedding-ada-002")
                .dimension(1536)
                .similarity("cosine")
                .build();
    }
    
    /**
     * 创建自定义配置
     */
    public static EmbeddingConfig of(String modelId, Integer dimension) {
        return EmbeddingConfig.builder()
                .modelId(modelId)
                .dimension(dimension)
                .similarity("cosine")
                .build();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingConfig that = (EmbeddingConfig) o;
        return Objects.equals(modelId, that.modelId) && 
               Objects.equals(dimension, that.dimension) &&
               Objects.equals(similarity, that.similarity);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(modelId, dimension, similarity);
    }
}
