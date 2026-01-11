package com.enterprise.rag.domain.model.knowledgebase;

/**
 * 知识库状态枚举
 */
public enum KnowledgeBaseStatus {
    
    /**
     * 创建中 - 索引正在初始化
     */
    CREATING,
    
    /**
     * 活跃 - 正常可用状态
     */
    ACTIVE,
    
    /**
     * 暂停 - 暂停接收数据
     */
    SUSPENDED,
    
    /**
     * 删除中 - 正在清理资源
     */
    DELETING,
    
    /**
     * 已删除 - 逻辑删除
     */
    DELETED
}
