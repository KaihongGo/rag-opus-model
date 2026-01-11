package com.enterprise.rag.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.enterprise.rag.domain.model.document.Document;
import com.enterprise.rag.domain.model.knowledgebase.FieldDefinition;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Elasticsearch 文档操作客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchDocumentClient {
    
    private final ElasticsearchClient esClient;
    
    /**
     * 索引单个文档
     */
    public void indexDocument(KnowledgeBase knowledgeBase, Document document, 
                              Map<String, float[]> vectors) throws IOException {
        String indexName = knowledgeBase.getIndexName();
        
        // 构建 ES 文档
        Map<String, Object> esDoc = buildEsDocument(knowledgeBase, document, vectors);
        
        IndexRequest<Map<String, Object>> request = IndexRequest.of(r -> r
                .index(indexName)
                .id(document.getId())
                .document(esDoc)
        );
        
        IndexResponse response = esClient.index(request);
        log.debug("Indexed document {} in index {}, result: {}", 
                document.getId(), indexName, response.result());
    }
    
    /**
     * 批量索引文档
     */
    public void bulkIndexDocuments(KnowledgeBase knowledgeBase, 
                                    List<Document> documents,
                                    Map<String, Map<String, float[]>> documentVectors) throws IOException {
        String indexName = knowledgeBase.getIndexName();
        
        List<BulkOperation> operations = new ArrayList<>();
        
        for (Document doc : documents) {
            Map<String, float[]> vectors = documentVectors.getOrDefault(doc.getId(), Map.of());
            Map<String, Object> esDoc = buildEsDocument(knowledgeBase, doc, vectors);
            
            operations.add(BulkOperation.of(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(doc.getId())
                            .document(esDoc)
                    )
            ));
        }
        
        if (!operations.isEmpty()) {
            BulkRequest request = BulkRequest.of(r -> r.operations(operations));
            BulkResponse response = esClient.bulk(request);
            
            if (response.errors()) {
                response.items().stream()
                        .filter(item -> item.error() != null)
                        .forEach(item -> log.error("Bulk index error: {}", item.error().reason()));
            }
            
            log.info("Bulk indexed {} documents in index {}", documents.size(), indexName);
        }
    }
    
    /**
     * 根据知识库元数据构建 ES 文档
     */
    private Map<String, Object> buildEsDocument(KnowledgeBase knowledgeBase, 
                                                 Document document,
                                                 Map<String, float[]> vectors) {
        Map<String, Object> esDoc = new HashMap<>();
        
        // 系统字段
        esDoc.put("_doc_id", document.getId());
        esDoc.put("_knowledge_base_id", document.getKnowledgeBaseId());
        esDoc.put("_created_at", formatDateTime(document.getCreatedAt()));
        esDoc.put("_updated_at", formatDateTime(document.getUpdatedAt()));
        esDoc.put("_source_document_id", document.getSourceDocumentId());
        esDoc.put("_chunk_index", document.getChunkIndex());
        
        // 根据字段定义映射内容字段
        Map<String, Object> content = document.getContent();
        for (FieldDefinition fd : knowledgeBase.getFieldDefinitionList()) {
            String fieldName = fd.getFieldName();
            
            if (content.containsKey(fieldName)) {
                esDoc.put(fieldName, content.get(fieldName));
            }
            
            // 如果是向量字段，添加向量值
            if (fd.isVectorField() && vectors.containsKey(fieldName)) {
                esDoc.put(fd.getVectorFieldName(), vectors.get(fieldName));
            }
        }
        
        // 元数据
        if (document.getMetadata() != null) {
            esDoc.put("_metadata", document.getMetadata());
        }
        
        return esDoc;
    }
    
    /**
     * 根据 ID 获取文档
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getDocument(String indexName, String documentId) throws IOException {
        GetRequest request = GetRequest.of(r -> r
                .index(indexName)
                .id(documentId)
        );
        
        GetResponse<Map> response = esClient.get(request, Map.class);
        
        if (response.found()) {
            return Optional.ofNullable(response.source());
        }
        return Optional.empty();
    }
    
    /**
     * 删除文档
     */
    public void deleteDocument(String indexName, String documentId) throws IOException {
        DeleteRequest request = DeleteRequest.of(r -> r
                .index(indexName)
                .id(documentId)
        );
        
        esClient.delete(request);
        log.debug("Deleted document {} from index {}", documentId, indexName);
    }
    
    /**
     * 根据过滤条件删除文档
     */
    public void deleteByQuery(String indexName, Map<String, Object> filters) throws IOException {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        
        filters.forEach((field, value) -> {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field(field).value(v -> v.stringValue(String.valueOf(value))))));
        });
        
        DeleteByQueryRequest request = DeleteByQueryRequest.of(r -> r
                .index(indexName)
                .query(Query.of(q -> q.bool(boolBuilder.build())))
        );
        
        esClient.deleteByQuery(request);
    }
    
    /**
     * 执行搜索请求
     */
    @SuppressWarnings("unchecked")
    public SearchResponse<Map> search(SearchRequest request) throws IOException {
        return esClient.search(request, Map.class);
    }
    
    /**
     * 从搜索结果中提取文档
     */
    public List<Map<String, Object>> extractDocuments(SearchResponse<Map> response) {
        List<Map<String, Object>> documents = new ArrayList<>();
        
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("_id", hit.id());
            doc.put("_score", hit.score());
            if (hit.source() != null) {
                doc.putAll(hit.source());
            }
            if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                doc.put("_highlight", hit.highlight());
            }
            documents.add(doc);
        }
        
        return documents;
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
