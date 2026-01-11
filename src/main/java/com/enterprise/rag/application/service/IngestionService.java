package com.enterprise.rag.application.service;

import com.enterprise.rag.domain.model.document.Document;
import com.enterprise.rag.domain.model.knowledgebase.FieldDefinition;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBaseRepository;
import com.enterprise.rag.domain.service.EmbeddingService;
import com.enterprise.rag.infrastructure.elasticsearch.ElasticsearchDocumentClient;
import com.enterprise.rag.infrastructure.elasticsearch.ElasticsearchIndexManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据摄取服务
 * 
 * <p>处理文档的摄取、向量化和索引</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {
    
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EmbeddingService embeddingService;
    private final ElasticsearchDocumentClient esDocumentClient;
    private final ElasticsearchIndexManager esIndexManager;
    
    /**
     * 摄取单个文档
     */
    @Transactional
    public void ingestDocument(String knowledgeBaseId, String documentId,
                               Map<String, Object> content, Map<String, Object> metadata,
                               String sourceDocumentId, Integer chunkIndex) {
        // 获取知识库
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
        
        // 检查知识库状态
        if (!knowledgeBase.canIngest()) {
            throw new IllegalStateException("Knowledge base is not accepting data: " + knowledgeBase.getStatus());
        }
        
        // 校验入站数据
        knowledgeBase.validateInboundData(content);
        
        // 创建文档
        String docId = documentId != null ? documentId : UUID.randomUUID().toString();
        Document document = Document.builder()
                .id(docId)
                .knowledgeBaseId(knowledgeBaseId)
                .content(content)
                .metadata(metadata)
                .sourceDocumentId(sourceDocumentId)
                .chunkIndex(chunkIndex)
                .createdAt(LocalDateTime.now())
                .build();
        
        // 生成向量
        Map<String, float[]> vectors = generateVectors(knowledgeBase, content);
        
        // 索引到 ES
        try {
            esDocumentClient.indexDocument(knowledgeBase, document, vectors);
            log.info("Successfully ingested document {} into knowledge base {}", docId, knowledgeBaseId);
        } catch (IOException e) {
            log.error("Failed to index document {} into ES", docId, e);
            throw new RuntimeException("Failed to index document", e);
        }
    }
    
    /**
     * 批量摄取文档
     */
    @Transactional
    public void ingestDocuments(String knowledgeBaseId, List<Map<String, Object>> documents) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
        
        if (!knowledgeBase.canIngest()) {
            throw new IllegalStateException("Knowledge base is not accepting data: " + knowledgeBase.getStatus());
        }
        
        List<Document> docList = new ArrayList<>();
        Map<String, Map<String, float[]>> documentVectors = new HashMap<>();
        
        for (Map<String, Object> docData : documents) {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) docData.get("content");
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) docData.get("metadata");
            
            String docId = (String) docData.getOrDefault("documentId", UUID.randomUUID().toString());
            
            Document document = Document.builder()
                    .id(docId)
                    .knowledgeBaseId(knowledgeBaseId)
                    .content(content)
                    .metadata(metadata)
                    .sourceDocumentId((String) docData.get("sourceDocumentId"))
                    .chunkIndex((Integer) docData.get("chunkIndex"))
                    .createdAt(LocalDateTime.now())
                    .build();
            
            docList.add(document);
            documentVectors.put(docId, generateVectors(knowledgeBase, content));
        }
        
        try {
            esDocumentClient.bulkIndexDocuments(knowledgeBase, docList, documentVectors);
            log.info("Successfully bulk ingested {} documents into knowledge base {}", 
                    documents.size(), knowledgeBaseId);
        } catch (IOException e) {
            log.error("Failed to bulk index documents into ES", e);
            throw new RuntimeException("Failed to bulk index documents", e);
        }
    }
    
    /**
     * 根据知识库定义生成向量
     */
    private Map<String, float[]> generateVectors(KnowledgeBase knowledgeBase, Map<String, Object> content) {
        Map<String, float[]> vectors = new HashMap<>();
        
        List<FieldDefinition> vectorFields = knowledgeBase.getVectorFields();
        
        for (FieldDefinition fd : vectorFields) {
            String fieldName = fd.getFieldName();
            Object fieldValue = content.get(fieldName);
            
            if (fieldValue != null) {
                String textToEmbed = String.valueOf(fieldValue);
                String modelId = fd.getEmbeddingConfig().getModelId();
                
                float[] vector = embeddingService.embed(textToEmbed, modelId);
                vectors.put(fieldName, vector);
                
                log.debug("Generated vector for field {} using model {}, dimension: {}", 
                        fieldName, modelId, vector.length);
            }
        }
        
        return vectors;
    }
    
    /**
     * 删除文档
     */
    public void deleteDocument(String knowledgeBaseId, String documentId) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
        
        try {
            esDocumentClient.deleteDocument(knowledgeBase.getIndexName(), documentId);
            log.info("Deleted document {} from knowledge base {}", documentId, knowledgeBaseId);
        } catch (IOException e) {
            log.error("Failed to delete document {}", documentId, e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
    
    /**
     * 更新文档
     */
    @Transactional
    public void updateDocument(String knowledgeBaseId, String documentId,
                               Map<String, Object> content, Map<String, Object> metadata) {
        // 更新实际上是删除后重新索引
        deleteDocument(knowledgeBaseId, documentId);
        ingestDocument(knowledgeBaseId, documentId, content, metadata, null, null);
    }
}
