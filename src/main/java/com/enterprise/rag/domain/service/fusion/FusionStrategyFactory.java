package com.enterprise.rag.domain.service.fusion;

import com.enterprise.rag.domain.model.search.FusionStrategyType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 融合策略工厂
 */
@Component
public class FusionStrategyFactory {
    
    private final Map<FusionStrategyType, FusionStrategy> strategyMap;
    
    public FusionStrategyFactory(List<FusionStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        FusionStrategy::getType,
                        Function.identity()
                ));
    }
    
    /**
     * 获取融合策略
     */
    public FusionStrategy getStrategy(FusionStrategyType type) {
        FusionStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported fusion strategy type: " + type);
        }
        return strategy;
    }
    
    /**
     * 检查是否支持该融合类型
     */
    public boolean supports(FusionStrategyType type) {
        return strategyMap.containsKey(type);
    }
}
