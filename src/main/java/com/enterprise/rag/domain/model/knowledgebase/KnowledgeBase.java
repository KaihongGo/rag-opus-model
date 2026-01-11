package com.enterprise.rag.domain.model.knowledgebase;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库聚合根
 * 
 * <p>知识库是 RAG 系统的核心聚合根，定义了数据的元结构和检索配置。
 * 每个知识库对应一个 Elasticsearch 索引，通过字段定义来驱动动态的数据摄取和检索行为。</p>
 */
@Getter
public class KnowledgeBase {
    
    /**
     * 知识库唯一标识
     */
    private final String id;
    
    /**
     * 知识库名称
     */
    private String name;
    
    /**
     * 知识库描述
     */
    private String description;
    
    /**
     * 租户 ID (多租户支持)
     */
    private final String tenantId;
    
    /**
     * 知识库状态
     */
    private KnowledgeBaseStatus status;
    
    /**
     * 字段定义集合
     */
    private final Map<String, FieldDefinition> fieldDefinitions;
    
    /**
     * Elasticsearch 索引名称
     */
    private final String indexName;
    
    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 版本号 (乐观锁)
     */
    private Long version;
    
    @Builder
    public KnowledgeBase(String id, String name, String description, String tenantId,
                         KnowledgeBaseStatus status, List<FieldDefinition> fieldDefinitions,
                         String indexName, LocalDateTime createdAt, LocalDateTime updatedAt,
                         Long version) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.tenantId = tenantId;
        this.status = status != null ? status : KnowledgeBaseStatus.CREATING;
        this.fieldDefinitions = new LinkedHashMap<>();
        if (fieldDefinitions != null) {
            fieldDefinitions.forEach(fd -> this.fieldDefinitions.put(fd.getFieldName(), fd));
        }
        this.indexName = indexName != null ? indexName : generateIndexName(tenantId, this.id);
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }
    
    /**
     * 添加字段定义
     */
    public void addFieldDefinition(FieldDefinition fieldDefinition) {
        fieldDefinition.validate();
        if (this.fieldDefinitions.containsKey(fieldDefinition.getFieldName())) {
            throw new IllegalArgumentException("Field already exists: " + fieldDefinition.getFieldName());
        }
        this.fieldDefinitions.put(fieldDefinition.getFieldName(), fieldDefinition);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 更新字段定义
     */
    public void updateFieldDefinition(FieldDefinition fieldDefinition) {
        fieldDefinition.validate();
        if (!this.fieldDefinitions.containsKey(fieldDefinition.getFieldName())) {
            throw new IllegalArgumentException("Field not found: " + fieldDefinition.getFieldName());
        }
        this.fieldDefinitions.put(fieldDefinition.getFieldName(), fieldDefinition);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 移除字段定义
     */
    public void removeFieldDefinition(String fieldName) {
        if (!this.fieldDefinitions.containsKey(fieldName)) {
            throw new IllegalArgumentException("Field not found: " + fieldName);
        }
        this.fieldDefinitions.remove(fieldName);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 获取所有字段定义
     */
    public List<FieldDefinition> getFieldDefinitionList() {
        return new ArrayList<>(fieldDefinitions.values());
    }
    
    /**
     * 获取指定字段定义
     */
    public Optional<FieldDefinition> getFieldDefinition(String fieldName) {
        return Optional.ofNullable(fieldDefinitions.get(fieldName));
    }
    
    /**
     * 获取所有向量字段
     */
    public List<FieldDefinition> getVectorFields() {
        return fieldDefinitions.values().stream()
                .filter(FieldDefinition::isVectorField)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有文本字段
     */
    public List<FieldDefinition> getTextFields() {
        return fieldDefinitions.values().stream()
                .filter(FieldDefinition::isTextField)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有关键词字段
     */
    public List<FieldDefinition> getKeywordFields() {
        return fieldDefinitions.values().stream()
                .filter(FieldDefinition::isKeywordField)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有可过滤字段
     */
    public List<FieldDefinition> getFilterableFields() {
        return fieldDefinitions.values().stream()
                .filter(FieldDefinition::isFilter)
                .collect(Collectors.toList());
    }
    
    /**
     * 激活知识库
     */
    public void activate() {
        if (this.status == KnowledgeBaseStatus.DELETED) {
            throw new IllegalStateException("Cannot activate a deleted knowledge base");
        }
        this.status = KnowledgeBaseStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 暂停知识库
     */
    public void suspend() {
        if (this.status != KnowledgeBaseStatus.ACTIVE) {
            throw new IllegalStateException("Only active knowledge base can be suspended");
        }
        this.status = KnowledgeBaseStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 删除知识库
     */
    public void markAsDeleted() {
        this.status = KnowledgeBaseStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 更新基本信息
     */
    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 判断是否可接收数据
     */
    public boolean canIngest() {
        return this.status == KnowledgeBaseStatus.ACTIVE;
    }
    
    /**
     * 判断是否可检索
     */
    public boolean canSearch() {
        return this.status == KnowledgeBaseStatus.ACTIVE || 
               this.status == KnowledgeBaseStatus.SUSPENDED;
    }
    
    /**
     * 校验入站数据字段
     */
    public void validateInboundData(Map<String, Object> data) {
        // 检查必填字段
        fieldDefinitions.values().stream()
                .filter(FieldDefinition::isRequired)
                .forEach(fd -> {
                    if (!data.containsKey(fd.getFieldName())) {
                        throw new IllegalArgumentException("Required field missing: " + fd.getFieldName());
                    }
                });
    }
    
    /**
     * 生成索引名称
     */
    private static String generateIndexName(String tenantId, String kbId) {
        return String.format("rag_kb_%s_%s", 
                tenantId != null ? tenantId.toLowerCase() : "default",
                kbId.toLowerCase().replace("-", ""));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnowledgeBase that = (KnowledgeBase) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
