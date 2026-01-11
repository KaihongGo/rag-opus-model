package com.enterprise.rag.application.service;

import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBaseRepository;
import com.enterprise.rag.domain.model.search.SearchRequest;
import com.enterprise.rag.domain.model.search.SearchResult;
import com.enterprise.rag.domain.service.fusion.FusionProcessor;
import com.enterprise.rag.infrastructure.elasticsearch.ElasticsearchIndexManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * 搜索应用服务
 * 
 * <p>协调搜索流程，处理跨聚合根的业务逻辑</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final FusionProcessor fusionProcessor;
    private final ElasticsearchIndexManager esIndexManager;
    
    /**
     * 执行混合检索
     */
    @Transactional(readOnly = true)
    public SearchResult search(SearchRequest request) {
        log.debug("Executing search in knowledge base: {}", request.getKnowledgeBaseId());
        
        // 获取知识库
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(request.getKnowledgeBaseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Knowledge base not found: " + request.getKnowledgeBaseId()));
        
        // 检查知识库是否可检索
        if (!knowledgeBase.canSearch()) {
            throw new IllegalStateException("Knowledge base is not searchable: " + knowledgeBase.getStatus());
        }
        
        // 执行检索和融合
        SearchResult result = fusionProcessor.process(knowledgeBase, request);
        
        log.info("Search completed: {} results in {} ms, fusion strategy: {}",
                result.getResultCount(), result.getTook(), result.getFusionStrategy());
        
        return result;
    }
    
    /**
     * 获取知识库列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeBase> listKnowledgeBases(String tenantId) {
        return knowledgeBaseRepository.findByTenantId(tenantId);
    }
    
    /**
     * 获取知识库详情
     */
    @Transactional(readOnly = true)
    public KnowledgeBase getKnowledgeBase(String id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + id));
    }
    
    /**
     * 创建知识库
     */
    @Transactional
    public KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase) {
        // 保存到数据库
        KnowledgeBase saved = knowledgeBaseRepository.save(knowledgeBase);
        
        // 创建 ES 索引
        try {
            esIndexManager.createIndex(saved);
            saved.activate();
            return knowledgeBaseRepository.save(saved);
        } catch (IOException e) {
            log.error("Failed to create ES index for knowledge base: {}", saved.getId(), e);
            throw new RuntimeException("Failed to create ES index", e);
        }
    }
    
    /**
     * 删除知识库
     */
    @Transactional
    public void deleteKnowledgeBase(String id) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + id));
        
        try {
            // 删除 ES 索引
            esIndexManager.deleteIndex(knowledgeBase.getIndexName());
            
            // 标记为删除
            knowledgeBase.markAsDeleted();
            knowledgeBaseRepository.save(knowledgeBase);
            
            log.info("Deleted knowledge base: {}", id);
        } catch (IOException e) {
            log.error("Failed to delete ES index for knowledge base: {}", id, e);
            throw new RuntimeException("Failed to delete ES index", e);
        }
    }
}
