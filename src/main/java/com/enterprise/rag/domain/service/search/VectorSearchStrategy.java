package com.enterprise.rag.domain.service.search;

import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.enterprise.rag.domain.model.knowledgebase.FieldDefinition;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import com.enterprise.rag.domain.model.search.RetrievalMethod;
import com.enterprise.rag.domain.model.search.RetrievalMethodType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向量检索策略实现
 * 
 * <p>使用 Elasticsearch 8.x 的 knn 查询实现语义相似度搜索</p>
 */
@Slf4j
@Component
public class VectorSearchStrategy implements SearchStrategy {
    
    @Override
    public RetrievalMethodType getSupportedType() {
        return RetrievalMethodType.VECTOR;
    }
    
    @Override
    public SearchRequest.Builder buildSearchRequest(
            KnowledgeBase knowledgeBase,
            String queryText,
            RetrievalMethod retrievalMethod,
            float[] queryVector,
            int topK) {
        
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("Query vector is required for vector search");
        }
        
        // 确定向量字段
        String vectorFieldName = determineVectorField(knowledgeBase, retrievalMethod);
        
        // 获取 KNN 配置
        int numCandidates = retrievalMethod.getVectorConfig() != null 
                ? retrievalMethod.getVectorConfig().getNumCandidates() 
                : 100;
        
        Float similarity = retrievalMethod.getVectorConfig() != null
                ? retrievalMethod.getVectorConfig().getSimilarity()
                : null;
        
        // 构建 KNN 查询
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(knowledgeBase.getIndexName())
                .knn(buildKnnQuery(vectorFieldName, queryVector, topK, numCandidates, similarity))
                .size(topK);
        
        log.debug("Built vector search request for index {} with field {}, topK={}, numCandidates={}",
                knowledgeBase.getIndexName(), vectorFieldName, topK, numCandidates);
        
        return builder;
    }
    
    /**
     * 构建 KNN 查询
     */
    private KnnQuery buildKnnQuery(String vectorField, float[] queryVector, 
                                   int k, int numCandidates, Float similarity) {
        KnnQuery.Builder knnBuilder = new KnnQuery.Builder()
                .field(vectorField)
                .queryVector(toFloatList(queryVector))
                .k(k)
                .numCandidates(numCandidates);
        
        if (similarity != null) {
            knnBuilder.similarity(similarity.doubleValue());
        }
        
        return knnBuilder.build();
    }
    
    /**
     * 确定使用的向量字段
     */
    private String determineVectorField(KnowledgeBase knowledgeBase, RetrievalMethod retrievalMethod) {
        // 如果配置中指定了向量字段
        if (retrievalMethod.getVectorConfig() != null && 
            retrievalMethod.getVectorConfig().getVectorField() != null) {
            String fieldName = retrievalMethod.getVectorConfig().getVectorField();
            // 获取对应的向量存储字段名
            return knowledgeBase.getFieldDefinition(fieldName)
                    .filter(FieldDefinition::isVectorField)
                    .map(FieldDefinition::getVectorFieldName)
                    .orElse(fieldName);
        }
        
        // 如果目标字段列表中有指定
        if (retrievalMethod.getTargetFields() != null && !retrievalMethod.getTargetFields().isEmpty()) {
            String fieldName = retrievalMethod.getTargetFields().get(0);
            return knowledgeBase.getFieldDefinition(fieldName)
                    .filter(FieldDefinition::isVectorField)
                    .map(FieldDefinition::getVectorFieldName)
                    .orElse(fieldName + "_vector");
        }
        
        // 默认使用第一个向量字段
        List<FieldDefinition> vectorFields = knowledgeBase.getVectorFields();
        if (vectorFields.isEmpty()) {
            throw new IllegalStateException("No vector fields defined in knowledge base");
        }
        
        return vectorFields.get(0).getVectorFieldName();
    }
    
    /**
     * float[] 转 List<Float>
     */
    private List<Float> toFloatList(float[] array) {
        Float[] boxedArray = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            boxedArray[i] = array[i];
        }
        return List.of(boxedArray);
    }
}
