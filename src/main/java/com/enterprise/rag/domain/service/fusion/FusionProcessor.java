package com.enterprise.rag.domain.service.fusion;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import com.enterprise.rag.domain.model.search.*;
import com.enterprise.rag.domain.service.EmbeddingService;
import com.enterprise.rag.domain.service.search.SearchStrategy;
import com.enterprise.rag.domain.service.search.SearchStrategyFactory;
import com.enterprise.rag.infrastructure.elasticsearch.ElasticsearchDocumentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 融合处理器
 * 
 * <p>协调多种检索策略执行和结果融合</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FusionProcessor {
    
    private final SearchStrategyFactory searchStrategyFactory;
    private final FusionStrategyFactory fusionStrategyFactory;
    private final ElasticsearchDocumentClient esDocumentClient;
    private final EmbeddingService embeddingService;
    
    /**
     * 执行混合检索并融合结果
     */
    public SearchResult process(KnowledgeBase knowledgeBase, SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        request.validate();
        
        // 如果只有一个检索方法且不需要融合
        if (request.getRetrievalMethods().size() == 1 && 
            request.getFusionStrategy() == FusionStrategyType.NONE) {
            return executeSingleSearch(knowledgeBase, request);
        }
        
        // 执行多个检索方法
        Map<RetrievalMethodType, List<FusionStrategy.RankedDocument>> resultsByMethod = new HashMap<>();
        Map<RetrievalMethodType, Double> weights = new HashMap<>();
        
        // 为向量检索生成查询向量
        float[] queryVector = generateQueryVector(knowledgeBase, request);
        
        for (RetrievalMethod method : request.getRetrievalMethods()) {
            try {
                List<FusionStrategy.RankedDocument> docs = executeSearch(
                        knowledgeBase, request.getQueryText(), method, queryVector, request.getTopK() * 2
                );
                resultsByMethod.put(method.getType(), docs);
                weights.put(method.getType(), method.getWeight() != null ? method.getWeight() : 1.0);
                
            } catch (Exception e) {
                log.error("Failed to execute {} search: {}", method.getType(), e.getMessage());
                // 继续执行其他检索方法
            }
        }
        
        if (resultsByMethod.isEmpty()) {
            return SearchResult.builder()
                    .knowledgeBaseId(knowledgeBase.getId())
                    .items(List.of())
                    .totalHits(0L)
                    .took(System.currentTimeMillis() - startTime)
                    .fusionStrategy(request.getFusionStrategy())
                    .build();
        }
        
        // 执行融合
        FusionStrategy fusionStrategy = fusionStrategyFactory.getStrategy(request.getFusionStrategy());
        
        // 如果是 RRF 策略，设置 k 参数
        if (fusionStrategy instanceof RRFFusionStrategy rrfStrategy && request.getRrfK() != null) {
            rrfStrategy.setK(request.getRrfK());
        }
        
        List<SearchResultItem> fusedResults = fusionStrategy.fuse(
                resultsByMethod, weights, request.getTopK()
        );
        
        // 应用最小分数过滤
        if (request.getMinScore() != null) {
            fusedResults = fusedResults.stream()
                    .filter(item -> item.getScore() >= request.getMinScore())
                    .collect(Collectors.toList());
        }
        
        long took = System.currentTimeMillis() - startTime;
        
        return SearchResult.builder()
                .knowledgeBaseId(knowledgeBase.getId())
                .items(fusedResults)
                .totalHits((long) fusedResults.size())
                .took(took)
                .fusionStrategy(request.getFusionStrategy())
                .debugInfo(buildDebugInfo(resultsByMethod, weights))
                .build();
    }
    
    /**
     * 执行单个检索策略
     */
    private List<FusionStrategy.RankedDocument> executeSearch(
            KnowledgeBase knowledgeBase,
            String queryText,
            RetrievalMethod method,
            float[] queryVector,
            int topK) throws IOException {
        
        SearchStrategy strategy = searchStrategyFactory.getStrategy(method.getType());
        
        co.elastic.clients.elasticsearch.core.SearchRequest.Builder requestBuilder = 
                strategy.buildSearchRequest(knowledgeBase, queryText, method, queryVector, topK);
        
        // 添加过滤条件
        // 这里可以根据需要扩展添加过滤逻辑
        
        co.elastic.clients.elasticsearch.core.SearchRequest esRequest = requestBuilder.build();
        SearchResponse<Map> response = esDocumentClient.search(esRequest);
        
        return convertToRankedDocuments(response);
    }
    
    /**
     * 将 ES 响应转换为排名文档列表
     */
    @SuppressWarnings("unchecked")
    private List<FusionStrategy.RankedDocument> convertToRankedDocuments(SearchResponse<Map> response) {
        List<FusionStrategy.RankedDocument> docs = new ArrayList<>();
        
        int rank = 1;
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, List<String>> highlights = hit.highlight() != null 
                    ? hit.highlight() 
                    : Map.of();
            
            docs.add(new FusionStrategy.RankedDocument(
                    hit.id(),
                    hit.score() != null ? hit.score() : 0.0,
                    rank++,
                    hit.source() != null ? (Map<String, Object>) hit.source() : Map.of(),
                    highlights
            ));
        }
        
        return docs;
    }
    
    /**
     * 执行单一检索（无融合）
     */
    private SearchResult executeSingleSearch(KnowledgeBase knowledgeBase, SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        RetrievalMethod method = request.getRetrievalMethods().get(0);
        float[] queryVector = null;
        
        if (method.getType() == RetrievalMethodType.VECTOR) {
            queryVector = generateQueryVector(knowledgeBase, request);
        }
        
        try {
            List<FusionStrategy.RankedDocument> docs = executeSearch(
                    knowledgeBase, request.getQueryText(), method, queryVector, request.getTopK()
            );
            
            List<SearchResultItem> items = docs.stream()
                    .map(doc -> SearchResultItem.builder()
                            .documentId(doc.documentId())
                            .score(doc.score())
                            .content(doc.content())
                            .highlights(doc.highlights())
                            .methodScores(Map.of(method.getType(), doc.score()))
                            .methodRanks(Map.of(method.getType(), doc.rank()))
                            .build())
                    .collect(Collectors.toList());
            
            return SearchResult.builder()
                    .knowledgeBaseId(knowledgeBase.getId())
                    .items(items)
                    .totalHits((long) items.size())
                    .took(System.currentTimeMillis() - startTime)
                    .fusionStrategy(FusionStrategyType.NONE)
                    .build();
                    
        } catch (IOException e) {
            log.error("Search failed", e);
            throw new RuntimeException("Search execution failed", e);
        }
    }
    
    /**
     * 生成查询向量
     */
    private float[] generateQueryVector(KnowledgeBase knowledgeBase, SearchRequest request) {
        // 检查是否有向量检索方法
        boolean hasVectorSearch = request.getRetrievalMethods().stream()
                .anyMatch(m -> m.getType() == RetrievalMethodType.VECTOR);
        
        if (!hasVectorSearch) {
            return null;
        }
        
        // 获取第一个向量字段的模型 ID
        String modelId = knowledgeBase.getVectorFields().stream()
                .findFirst()
                .map(fd -> fd.getEmbeddingConfig().getModelId())
                .orElse(null);
        
        return embeddingService.embed(request.getQueryText(), modelId);
    }
    
    /**
     * 构建调试信息
     */
    private Map<String, Object> buildDebugInfo(
            Map<RetrievalMethodType, List<FusionStrategy.RankedDocument>> resultsByMethod,
            Map<RetrievalMethodType, Double> weights) {
        
        Map<String, Object> debug = new HashMap<>();
        
        Map<String, Object> methodInfo = new HashMap<>();
        for (Map.Entry<RetrievalMethodType, List<FusionStrategy.RankedDocument>> entry : resultsByMethod.entrySet()) {
            methodInfo.put(entry.getKey().name(), Map.of(
                    "count", entry.getValue().size(),
                    "weight", weights.getOrDefault(entry.getKey(), 1.0)
            ));
        }
        debug.put("methods", methodInfo);
        
        return debug;
    }
}
