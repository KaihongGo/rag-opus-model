package com.enterprise.rag.domain.service.fusion;

import com.enterprise.rag.domain.model.search.FusionStrategyType;
import com.enterprise.rag.domain.model.search.RetrievalMethodType;
import com.enterprise.rag.domain.model.search.SearchResultItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RRF (Reciprocal Rank Fusion) 融合策略实现
 * 
 * <p>使用倒数排名融合算法，该算法对分数分布不敏感，
 * 仅依赖排名信息进行融合。</p>
 * 
 * <pre>
 * RRF_score(d) = Σ 1 / (k + rank_i(d))
 * 
 * 其中:
 * - k 是一个常数参数（默认 60）
 * - rank_i(d) 是文档 d 在第 i 个检索方法中的排名
 * </pre>
 * 
 * <p>参考论文: Cormack, Clarke, Buettcher (2009) - 
 * "Reciprocal Rank Fusion Outperforms Condorcet and Individual Rank Learning Methods"</p>
 */
@Slf4j
@Component
public class RRFFusionStrategy implements FusionStrategy {
    
    /**
     * 默认 RRF k 参数
     * k 值越大，排名差异的影响越小
     */
    private static final int DEFAULT_K = 60;
    
    private int k = DEFAULT_K;
    
    @Override
    public FusionStrategyType getType() {
        return FusionStrategyType.RRF;
    }
    
    /**
     * 设置 RRF k 参数
     */
    public void setK(int k) {
        this.k = k;
    }
    
    @Override
    public List<SearchResultItem> fuse(
            Map<RetrievalMethodType, List<RankedDocument>> resultsByMethod,
            Map<RetrievalMethodType, Double> weights,
            int topK) {
        
        if (resultsByMethod.isEmpty()) {
            return List.of();
        }
        
        // 收集所有唯一文档并计算 RRF 分数
        Map<String, DocumentRRFAggregator> aggregators = new HashMap<>();
        
        for (Map.Entry<RetrievalMethodType, List<RankedDocument>> entry : resultsByMethod.entrySet()) {
            RetrievalMethodType method = entry.getKey();
            List<RankedDocument> docs = entry.getValue();
            
            for (int i = 0; i < docs.size(); i++) {
                RankedDocument doc = docs.get(i);
                int rank = i + 1; // 排名从 1 开始
                
                aggregators.computeIfAbsent(doc.documentId(), id -> new DocumentRRFAggregator(doc))
                        .addRank(method, rank, doc.score());
            }
        }
        
        // 计算 RRF 分数并排序
        List<SearchResultItem> results = aggregators.values().stream()
                .map(agg -> agg.toSearchResultItem(k))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
        
        log.debug("RRF fusion (k={}): {} methods, {} unique docs -> {} results",
                k, resultsByMethod.size(), aggregators.size(), results.size());
        
        return results;
    }
    
    /**
     * 支持自定义 k 值的融合方法
     */
    public List<SearchResultItem> fuse(
            Map<RetrievalMethodType, List<RankedDocument>> resultsByMethod,
            Map<RetrievalMethodType, Double> weights,
            int topK,
            int rrfK) {
        this.k = rrfK;
        return fuse(resultsByMethod, weights, topK);
    }
    
    /**
     * 文档 RRF 分数聚合器
     */
    private static class DocumentRRFAggregator {
        private final String documentId;
        private final Map<String, Object> content;
        private final Map<String, List<String>> highlights;
        private final Map<RetrievalMethodType, Integer> methodRanks = new HashMap<>();
        private final Map<RetrievalMethodType, Double> methodScores = new HashMap<>();
        
        public DocumentRRFAggregator(RankedDocument doc) {
            this.documentId = doc.documentId();
            this.content = doc.content();
            this.highlights = doc.highlights();
        }
        
        public void addRank(RetrievalMethodType method, int rank, Double originalScore) {
            methodRanks.put(method, rank);
            methodScores.put(method, originalScore);
        }
        
        public SearchResultItem toSearchResultItem(int k) {
            // 计算 RRF 分数
            double rrfScore = methodRanks.values().stream()
                    .mapToDouble(rank -> 1.0 / (k + rank))
                    .sum();
            
            return SearchResultItem.builder()
                    .documentId(documentId)
                    .score(rrfScore)
                    .methodScores(new HashMap<>(methodScores))
                    .methodRanks(new HashMap<>(methodRanks))
                    .content(content)
                    .highlights(highlights)
                    .build();
        }
    }
}
