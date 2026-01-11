package com.enterprise.rag.domain.model.knowledgebase;

import java.util.Optional;

/**
 * 知识库仓储接口 - 领域层定义
 */
public interface KnowledgeBaseRepository {
    
    /**
     * 保存知识库
     */
    KnowledgeBase save(KnowledgeBase knowledgeBase);
    
    /**
     * 根据 ID 查找
     */
    Optional<KnowledgeBase> findById(String id);
    
    /**
     * 根据索引名称查找
     */
    Optional<KnowledgeBase> findByIndexName(String indexName);
    
    /**
     * 根据租户 ID 查找所有
     */
    java.util.List<KnowledgeBase> findByTenantId(String tenantId);
    
    /**
     * 删除知识库
     */
    void delete(String id);
    
    /**
     * 检查是否存在
     */
    boolean existsById(String id);
}
