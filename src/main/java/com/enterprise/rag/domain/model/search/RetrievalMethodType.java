package com.enterprise.rag.domain.model.search;

/**
 * 检索方法类型枚举
 */
public enum RetrievalMethodType {
    
    /**
     * 向量语义检索 - 使用 ES knn 查询
     */
    VECTOR,
    
    /**
     * 全文文本检索 - 使用 ES match/multi_match 查询
     */
    TEXT,
    
    /**
     * 混合检索 - 同时使用向量和文本检索
     */
    HYBRID
}
