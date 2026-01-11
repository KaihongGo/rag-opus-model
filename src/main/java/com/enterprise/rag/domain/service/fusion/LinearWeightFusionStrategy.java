package com.enterprise.rag.domain.service.fusion;

import com.enterprise.rag.domain.model.search.FusionStrategyType;
import com.enterprise.rag.domain.model.search.RetrievalMethodType;
import com.enterprise.rag.domain.model.search.SearchResultItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 线性加权融合策略实现
 * 
 * <p>使用归一化分数和权重进行线性组合</p>
 * 
 * <pre>
 * final_score = Σ(weight_i * normalized_score_i)
 * </pre>
 */
@Slf4j
@Component
public class LinearWeightFusionStrategy implements FusionStrategy {
    
    @Override
    public FusionStrategyType getType() {
        return FusionStrategyType.LINEAR_WEIGHT;
    }
    
    @Override
    public List<SearchResultItem> fuse(
            Map<RetrievalMethodType, List<RankedDocument>> resultsByMethod,
            Map<RetrievalMethodType, Double> weights,
            int topK) {
        
        if (resultsByMethod.isEmpty()) {
            return List.of();
        }
        
        // 归一化权重
        Map<RetrievalMethodType, Double> normalizedWeights = normalizeWeights(weights, resultsByMethod.keySet());
        
        // 归一化每个方法的分数
        Map<RetrievalMethodType, List<RankedDocument>> normalizedResults = new HashMap<>();
        for (Map.Entry<RetrievalMethodType, List<RankedDocument>> entry : resultsByMethod.entrySet()) {
            normalizedResults.put(entry.getKey(), normalizeScores(entry.getValue()));
        }
        
        // 收集所有唯一文档
        Map<String, DocumentScoreAggregator> aggregators = new HashMap<>();
        
        for (Map.Entry<RetrievalMethodType, List<RankedDocument>> entry : normalizedResults.entrySet()) {
            RetrievalMethodType method = entry.getKey();
            Double weight = normalizedWeights.get(method);
            List<RankedDocument> docs = entry.getValue();
            
            for (int rank = 0; rank < docs.size(); rank++) {
                RankedDocument doc = docs.get(rank);
                
                aggregators.computeIfAbsent(doc.documentId(), id -> new DocumentScoreAggregator(doc))
                        .addScore(method, doc.score() * weight, rank + 1);
            }
        }
        
        // 计算最终分数并排序
        List<SearchResultItem> results = aggregators.values().stream()
                .map(agg -> agg.toSearchResultItem())
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
        
        log.debug("Linear weight fusion: {} methods, {} unique docs -> {} results",
                resultsByMethod.size(), aggregators.size(), results.size());
        
        return results;
    }
    
    /**
     * 归一化权重，使总和为 1
     */
    private Map<RetrievalMethodType, Double> normalizeWeights(
            Map<RetrievalMethodType, Double> weights,
            Set<RetrievalMethodType> methods) {
        
        Map<RetrievalMethodType, Double> normalized = new HashMap<>();
        double sum = 0;
        
        for (RetrievalMethodType method : methods) {
            double weight = weights.getOrDefault(method, 1.0);
            normalized.put(method, weight);
            sum += weight;
        }
        
        // 归一化
        final double finalSum = sum;
        normalized.replaceAll((k, v) -> v / finalSum);
        
        return normalized;
    }
    
    /**
     * Min-Max 归一化分数到 [0, 1] 区间
     */
    private List<RankedDocument> normalizeScores(List<RankedDocument> docs) {
        if (docs.isEmpty()) {
            return docs;
        }
        
        // 找出最大最小分数
        double minScore = docs.stream()
                .mapToDouble(RankedDocument::score)
                .min()
                .orElse(0);
        double maxScore = docs.stream()
                .mapToDouble(RankedDocument::score)
                .max()
                .orElse(1);
        
        double range = maxScore - minScore;
        if (range == 0) {
            range = 1; // 避免除零
        }
        
        final double finalRange = range;
        final double finalMinScore = minScore;
        
        return docs.stream()
                .map(doc -> new RankedDocument(
                        doc.documentId(),
                        (doc.score() - finalMinScore) / finalRange,
                        doc.rank(),
                        doc.content(),
                        doc.highlights()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 文档分数聚合器
     */
    private static class DocumentScoreAggregator {
        private final String documentId;
        private final Map<String, Object> content;
        private final Map<String, List<String>> highlights;
        private final Map<RetrievalMethodType, Double> methodScores = new HashMap<>();
        private final Map<RetrievalMethodType, Integer> methodRanks = new HashMap<>();
        
        public DocumentScoreAggregator(RankedDocument doc) {
            this.documentId = doc.documentId();
            this.content = doc.content();
            this.highlights = doc.highlights();
        }
        
        public void addScore(RetrievalMethodType method, Double score, Integer rank) {
            methodScores.put(method, score);
            methodRanks.put(method, rank);
        }
        
        public SearchResultItem toSearchResultItem() {
            double finalScore = methodScores.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            
            return SearchResultItem.builder()
                    .documentId(documentId)
                    .score(finalScore)
                    .methodScores(new HashMap<>(methodScores))
                    .methodRanks(new HashMap<>(methodRanks))
                    .content(content)
                    .highlights(highlights)
                    .build();
        }
    }
}
