package com.enterprise.rag.domain.model.knowledgebase;

/**
 * 字段索引类型枚举
 * 
 * <p>定义字段在 Elasticsearch 中的索引方式</p>
 */
public enum IndexType {
    
    /**
     * 向量字段 - 需要向量化处理，用于语义相似度搜索
     */
    VECTOR,
    
    /**
     * 文本字段 - 全文索引，用于文本匹配搜索
     */
    TEXT,
    
    /**
     * 关键词字段 - 仅存储/过滤，精确匹配
     */
    KEYWORD
}
