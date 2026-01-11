package com.enterprise.rag.domain.service.search;

import com.enterprise.rag.domain.model.search.RetrievalMethodType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 检索策略工厂
 * 
 * <p>管理和分发检索策略实现</p>
 */
@Component
public class SearchStrategyFactory {
    
    private final Map<RetrievalMethodType, SearchStrategy> strategyMap;
    
    public SearchStrategyFactory(List<SearchStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        SearchStrategy::getSupportedType,
                        Function.identity()
                ));
    }
    
    /**
     * 获取检索策略
     */
    public SearchStrategy getStrategy(RetrievalMethodType type) {
        SearchStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported retrieval method type: " + type);
        }
        return strategy;
    }
    
    /**
     * 检查是否支持该检索类型
     */
    public boolean supports(RetrievalMethodType type) {
        return strategyMap.containsKey(type);
    }
    
    /**
     * 获取所有支持的检索类型
     */
    public List<RetrievalMethodType> getSupportedTypes() {
        return List.copyOf(strategyMap.keySet());
    }
}
