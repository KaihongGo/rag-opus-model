package com.enterprise.rag.domain.model.search;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 搜索结果 - 值对象
 */
@Getter
@Builder
public class SearchResult {
    
    /**
     * 知识库 ID
     */
    private final String knowledgeBaseId;
    
    /**
     * 结果列表
     */
    private final List<SearchResultItem> items;
    
    /**
     * 总命中数
     */
    private final Long totalHits;
    
    /**
     * 查询耗时 (毫秒)
     */
    private final Long took;
    
    /**
     * 使用的融合策略
     */
    private final FusionStrategyType fusionStrategy;
    
    /**
     * 调试信息
     */
    private final Map<String, Object> debugInfo;
    
    /**
     * 是否还有更多结果
     */
    public boolean hasMore() {
        return totalHits != null && items != null && totalHits > items.size();
    }
    
    /**
     * 获取结果数量
     */
    public int getResultCount() {
        return items != null ? items.size() : 0;
    }
}
