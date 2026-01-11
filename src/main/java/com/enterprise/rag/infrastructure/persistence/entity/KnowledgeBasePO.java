package com.enterprise.rag.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库数据库实体
 */
@Data
@TableName("knowledge_base")
public class KnowledgeBasePO {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String name;
    
    private String description;
    
    private String tenantId;
    
    private String status;
    
    /**
     * 字段定义 JSON
     */
    private String fieldDefinitions;
    
    private String indexName;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    @TableLogic
    private Integer deleted;
}
