package com.enterprise.rag.domain.model.search;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 搜索结果项 - 值对象
 */
@Getter
@Builder
public class SearchResultItem {
    
    /**
     * 文档 ID
     */
    private final String documentId;
    
    /**
     * 最终融合分数
     */
    private final Double score;
    
    /**
     * 各检索方法的原始分数
     */
    private final Map<RetrievalMethodType, Double> methodScores;
    
    /**
     * 各检索方法的排名
     */
    private final Map<RetrievalMethodType, Integer> methodRanks;
    
    /**
     * 文档内容
     */
    private final Map<String, Object> content;
    
    /**
     * 高亮结果
     */
    private final Map<String, List<String>> highlights;
    
    /**
     * 元数据
     */
    private final Map<String, Object> metadata;
}
