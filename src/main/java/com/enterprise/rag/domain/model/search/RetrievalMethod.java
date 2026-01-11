package com.enterprise.rag.domain.model.search;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 检索方法配置 - 值对象
 * 
 * <p>定义单个检索方法的完整配置</p>
 */
@Getter
@Builder
public class RetrievalMethod {
    
    /**
     * 检索方法类型
     */
    private final RetrievalMethodType type;
    
    /**
     * 权重 (用于线性加权融合)
     */
    private final Double weight;
    
    /**
     * 目标字段列表
     */
    private final List<String> targetFields;
    
    /**
     * 向量检索配置
     */
    private final VectorSearchConfig vectorConfig;
    
    /**
     * 文本检索配置
     */
    private final TextSearchConfig textConfig;
    
    /**
     * 最小相关性分数
     */
    private final Float minScore;
    
    /**
     * 向量检索配置
     */
    @Getter
    @Builder
    public static class VectorSearchConfig {
        /**
         * 查询向量 (可选，如果提供则直接使用)
         */
        private final float[] queryVector;
        
        /**
         * 向量字段名
         */
        private final String vectorField;
        
        /**
         * KNN 候选数量
         */
        @Builder.Default
        private final Integer numCandidates = 100;
        
        /**
         * 相似度阈值
         */
        @Builder.Default
        private final Float similarity = 0.7f;
    }
    
    /**
     * 文本检索配置
     */
    @Getter
    @Builder
    public static class TextSearchConfig {
        /**
         * 匹配类型: best_fields, most_fields, cross_fields, phrase
         */
        @Builder.Default
        private final String matchType = "best_fields";
        
        /**
         * 字段权重 (boost)
         */
        private final Map<String, Float> fieldBoosts;
        
        /**
         * 模糊匹配
         */
        @Builder.Default
        private final String fuzziness = "AUTO";
        
        /**
         * 分析器
         */
        private final String analyzer;
    }
    
    /**
     * 创建向量检索方法
     */
    public static RetrievalMethod vector(String vectorField, Double weight) {
        return RetrievalMethod.builder()
                .type(RetrievalMethodType.VECTOR)
                .weight(weight)
                .vectorConfig(VectorSearchConfig.builder()
                        .vectorField(vectorField)
                        .build())
                .build();
    }
    
    /**
     * 创建文本检索方法
     */
    public static RetrievalMethod text(List<String> fields, Double weight) {
        return RetrievalMethod.builder()
                .type(RetrievalMethodType.TEXT)
                .weight(weight)
                .targetFields(fields)
                .build();
    }
}
