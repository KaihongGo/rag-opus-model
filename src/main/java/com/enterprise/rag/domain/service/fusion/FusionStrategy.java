package com.enterprise.rag.domain.service.fusion;

import com.enterprise.rag.domain.model.search.FusionStrategyType;
import com.enterprise.rag.domain.model.search.RetrievalMethodType;
import com.enterprise.rag.domain.model.search.SearchResultItem;

import java.util.List;
import java.util.Map;

/**
 * 融合策略接口
 * 
 * <p>定义不同评分融合算法的抽象接口</p>
 */
public interface FusionStrategy {
    
    /**
     * 获取融合策略类型
     */
    FusionStrategyType getType();
    
    /**
     * 融合多个检索结果
     * 
     * @param resultsByMethod 各检索方法的结果 (检索方法类型 -> 文档列表)
     * @param weights 各检索方法的权重
     * @param topK 返回数量
     * @return 融合后的结果列表
     */
    List<SearchResultItem> fuse(
            Map<RetrievalMethodType, List<RankedDocument>> resultsByMethod,
            Map<RetrievalMethodType, Double> weights,
            int topK
    );
    
    /**
     * 排名文档 - 用于中间结果
     */
    record RankedDocument(
            String documentId,
            Double score,
            Integer rank,
            Map<String, Object> content,
            Map<String, List<String>> highlights
    ) {}
}
