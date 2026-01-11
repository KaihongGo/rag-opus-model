package com.enterprise.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RAG Retrieval Platform - 元数据驱动的企业级 RAG 检索中台
 * 
 * <p>核心功能:
 * <ul>
 *   <li>动态元数据驱动的知识库管理</li>
 *   <li>多策略混合检索（向量、全文）</li>
 *   <li>可插拔的评分融合算法</li>
 *   <li>Kafka 驱动的异步数据摄取</li>
 * </ul>
 * </p>
 *
 * @author RAG Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
public class RagRetrievalPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagRetrievalPlatformApplication.class, args);
    }
}
