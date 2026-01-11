package com.enterprise.rag.domain.model.document;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 文档实体
 * 
 * <p>表示存储在知识库中的单个文档/文本块</p>
 */
@Getter
@Builder
public class Document {
    
    /**
     * 文档唯一标识
     */
    private final String id;
    
    /**
     * 所属知识库 ID
     */
    private final String knowledgeBaseId;
    
    /**
     * 文档内容字段 (动态，根据知识库定义)
     */
    private final Map<String, Object> content;
    
    /**
     * 向量字段 (字段名 -> 向量值)
     */
    private final Map<String, float[]> vectors;
    
    /**
     * 元数据
     */
    private final Map<String, Object> metadata;
    
    /**
     * 来源文档 ID (如果是切片)
     */
    private final String sourceDocumentId;
    
    /**
     * 切片索引
     */
    private final Integer chunkIndex;
    
    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private final LocalDateTime updatedAt;
    
    /**
     * 创建新文档
     */
    public static Document create(String knowledgeBaseId, Map<String, Object> content, 
                                   Map<String, Object> metadata) {
        return Document.builder()
                .id(UUID.randomUUID().toString())
                .knowledgeBaseId(knowledgeBaseId)
                .content(content)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
