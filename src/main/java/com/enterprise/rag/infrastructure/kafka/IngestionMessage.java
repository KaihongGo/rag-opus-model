package com.enterprise.rag.infrastructure.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * 摄取消息 DTO
 * 
 * <p>Kafka 消息格式定义</p>
 */
@Getter
@Builder
@Jacksonized
public class IngestionMessage {
    
    /**
     * 知识库 ID
     */
    private final String knowledgeBaseId;
    
    /**
     * 文档 ID (可选，如果不提供则自动生成)
     */
    private final String documentId;
    
    /**
     * 文档内容 (动态字段)
     */
    private final Map<String, Object> content;
    
    /**
     * 元数据
     */
    private final Map<String, Object> metadata;
    
    /**
     * 来源文档 ID
     */
    private final String sourceDocumentId;
    
    /**
     * 切片索引
     */
    private final Integer chunkIndex;
    
    /**
     * 操作类型: INDEX, DELETE, UPDATE
     */
    @Builder.Default
    private final OperationType operation = OperationType.INDEX;
    
    public enum OperationType {
        INDEX, DELETE, UPDATE
    }
}
