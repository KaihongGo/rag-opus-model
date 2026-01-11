package com.enterprise.rag.domain.model.search;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 搜索请求 - 值对象
 * 
 * <p>封装完整的检索请求参数</p>
 */
@Getter
@Builder
public class SearchRequest {
    
    /**
     * 知识库 ID
     */
    private final String knowledgeBaseId;
    
    /**
     * 查询文本
     */
    private final String queryText;
    
    /**
     * 检索方法列表
     */
    private final List<RetrievalMethod> retrievalMethods;
    
    /**
     * 融合策略
     */
    @Builder.Default
    private final FusionStrategyType fusionStrategy = FusionStrategyType.RRF;
    
    /**
     * RRF 参数 k (默认 60)
     */
    @Builder.Default
    private final Integer rrfK = 60;
    
    /**
     * 返回结果数量
     */
    @Builder.Default
    private final Integer topK = 10;
    
    /**
     * 最小分数阈值
     */
    private final Float minScore;
    
    /**
     * 过滤条件
     */
    private final Map<String, Object> filters;
    
    /**
     * 是否返回向量
     */
    @Builder.Default
    private final boolean includeVectors = false;
    
    /**
     * 是否返回元数据
     */
    @Builder.Default
    private final boolean includeMetadata = true;
    
    /**
     * 分页 - 起始位置
     */
    @Builder.Default
    private final Integer from = 0;
    
    /**
     * 高亮配置
     */
    private final HighlightConfig highlightConfig;
    
    /**
     * 高亮配置
     */
    @Getter
    @Builder
    public static class HighlightConfig {
        private final List<String> fields;
        @Builder.Default
        private final String preTag = "<em>";
        @Builder.Default
        private final String postTag = "</em>";
        @Builder.Default
        private final Integer fragmentSize = 150;
    }
    
    /**
     * 校验请求
     */
    public void validate() {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            throw new IllegalArgumentException("Knowledge base ID is required");
        }
        if (queryText == null || queryText.isBlank()) {
            throw new IllegalArgumentException("Query text is required");
        }
        if (retrievalMethods == null || retrievalMethods.isEmpty()) {
            throw new IllegalArgumentException("At least one retrieval method is required");
        }
        if (topK != null && (topK < 1 || topK > 1000)) {
            throw new IllegalArgumentException("TopK must be between 1 and 1000");
        }
    }
}
