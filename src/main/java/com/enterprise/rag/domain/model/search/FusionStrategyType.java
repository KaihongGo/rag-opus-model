package com.enterprise.rag.domain.model.search;

/**
 * 融合策略类型枚举
 */
public enum FusionStrategyType {
    
    /**
     * 线性加权融合
     */
    LINEAR_WEIGHT,
    
    /**
     * RRF (Reciprocal Rank Fusion) 融合
     */
    RRF,
    
    /**
     * 无融合 - 单一检索方法
     */
    NONE
}
