package com.enterprise.rag.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.enterprise.rag.domain.model.knowledgebase.FieldDefinition;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch 索引管理器
 * 
 * <p>根据知识库元数据动态创建和管理 ES 索引</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexManager {
    
    private final ElasticsearchClient esClient;
    
    @Value("${rag.index.number-of-shards:1}")
    private int numberOfShards;
    
    @Value("${rag.index.number-of-replicas:0}")
    private int numberOfReplicas;
    
    /**
     * 根据知识库元数据创建 ES 索引
     */
    public void createIndex(KnowledgeBase knowledgeBase) throws IOException {
        String indexName = knowledgeBase.getIndexName();
        
        // 检查索引是否已存在
        if (indexExists(indexName)) {
            log.warn("Index {} already exists, skipping creation", indexName);
            return;
        }
        
        // 构建动态 mapping
        Map<String, Property> properties = buildProperties(knowledgeBase);
        
        CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                .index(indexName)
                .settings(s -> s
                        .numberOfShards(String.valueOf(numberOfShards))
                        .numberOfReplicas(String.valueOf(numberOfReplicas))
                )
                .mappings(m -> m.properties(properties))
        );
        
        esClient.indices().create(request);
        log.info("Created ES index: {} with {} field definitions", 
                indexName, knowledgeBase.getFieldDefinitionList().size());
    }
    
    /**
     * 根据字段定义构建 ES properties
     */
    private Map<String, Property> buildProperties(KnowledgeBase knowledgeBase) {
        Map<String, Property> properties = new HashMap<>();
        
        // 系统字段
        properties.put("_doc_id", Property.of(p -> p.keyword(k -> k)));
        properties.put("_knowledge_base_id", Property.of(p -> p.keyword(k -> k)));
        properties.put("_created_at", Property.of(p -> p.date(d -> d)));
        properties.put("_updated_at", Property.of(p -> p.date(d -> d)));
        properties.put("_source_document_id", Property.of(p -> p.keyword(k -> k)));
        properties.put("_chunk_index", Property.of(p -> p.integer(i -> i)));
        
        // 根据字段定义动态构建
        for (FieldDefinition fd : knowledgeBase.getFieldDefinitionList()) {
            Property property = buildPropertyFromDefinition(fd);
            properties.put(fd.getFieldName(), property);
            
            // 如果是向量字段，额外创建向量存储字段
            if (fd.isVectorField()) {
                Property vectorProperty = buildDenseVectorProperty(fd);
                properties.put(fd.getVectorFieldName(), vectorProperty);
            }
        }
        
        return properties;
    }
    
    /**
     * 根据字段定义构建 ES Property
     */
    private Property buildPropertyFromDefinition(FieldDefinition fd) {
        return switch (fd.getIndexType()) {
            case TEXT -> Property.of(p -> p
                    .text(t -> t
                            .analyzer("standard")
                            .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))
                    ));
            case KEYWORD -> Property.of(p -> p.keyword(k -> k));
            case VECTOR -> Property.of(p -> p.text(t -> t)); // 原文存储
        };
    }
    
    /**
     * 构建 dense_vector 类型属性
     */
    private Property buildDenseVectorProperty(FieldDefinition fd) {
        int dimension = fd.getEmbeddingConfig().getDimension();
        String similarity = fd.getEmbeddingConfig().getSimilarity();
        
        return Property.of(p -> p
                .denseVector(dv -> dv
                        .dims(dimension)
                        .index(true)
                        .similarity(similarity)
                        .indexOptions(io -> io
                                .type("hnsw")
                                .m(16)
                                .efConstruction(100)
                        )
                ));
    }
    
    /**
     * 检查索引是否存在
     */
    public boolean indexExists(String indexName) throws IOException {
        ExistsRequest request = ExistsRequest.of(r -> r.index(indexName));
        return esClient.indices().exists(request).value();
    }
    
    /**
     * 删除索引
     */
    public void deleteIndex(String indexName) throws IOException {
        if (indexExists(indexName)) {
            DeleteIndexRequest request = DeleteIndexRequest.of(r -> r.index(indexName));
            esClient.indices().delete(request);
            log.info("Deleted ES index: {}", indexName);
        }
    }
    
    /**
     * 更新索引 mapping (添加新字段)
     */
    public void updateMapping(KnowledgeBase knowledgeBase) throws IOException {
        String indexName = knowledgeBase.getIndexName();
        Map<String, Property> properties = buildProperties(knowledgeBase);
        
        esClient.indices().putMapping(m -> m
                .index(indexName)
                .properties(properties)
        );
        
        log.info("Updated mapping for index: {}", indexName);
    }
}
