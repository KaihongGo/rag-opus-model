package com.enterprise.rag.domain.service.search;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import com.enterprise.rag.domain.model.search.RetrievalMethod;
import com.enterprise.rag.domain.model.search.RetrievalMethodType;

/**
 * 检索策略接口
 * 
 * <p>定义不同检索方式的抽象接口，支持策略模式扩展</p>
 */
public interface SearchStrategy {
    
    /**
     * 获取支持的检索方法类型
     */
    RetrievalMethodType getSupportedType();
    
    /**
     * 构建 ES 搜索请求
     * 
     * @param knowledgeBase 知识库元数据
     * @param queryText 查询文本
     * @param retrievalMethod 检索方法配置
     * @param queryVector 查询向量 (向量检索时使用)
     * @param topK 返回数量
     * @return ES SearchRequest Builder
     */
    SearchRequest.Builder buildSearchRequest(
            KnowledgeBase knowledgeBase,
            String queryText,
            RetrievalMethod retrievalMethod,
            float[] queryVector,
            int topK
    );
    
    /**
     * 判断是否支持该检索方法
     */
    default boolean supports(RetrievalMethodType type) {
        return getSupportedType().equals(type);
    }
}
