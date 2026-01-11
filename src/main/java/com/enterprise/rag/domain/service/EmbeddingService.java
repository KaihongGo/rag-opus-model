package com.enterprise.rag.domain.service;

import java.util.List;

/**
 * 嵌入向量服务接口 - 领域服务
 * 
 * <p>定义文本向量化能力的抽象接口</p>
 */
public interface EmbeddingService {
    
    /**
     * 生成单个文本的嵌入向量
     * 
     * @param text 输入文本
     * @param modelId 模型 ID
     * @return 嵌入向量
     */
    float[] embed(String text, String modelId);
    
    /**
     * 批量生成嵌入向量
     * 
     * @param texts 文本列表
     * @param modelId 模型 ID
     * @return 嵌入向量列表
     */
    List<float[]> embedBatch(List<String> texts, String modelId);
    
    /**
     * 获取模型的向量维度
     * 
     * @param modelId 模型 ID
     * @return 向量维度
     */
    int getDimension(String modelId);
}
