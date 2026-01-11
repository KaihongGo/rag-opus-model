package com.enterprise.rag.infrastructure.persistence.repository;

import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBaseRepository;
import com.enterprise.rag.infrastructure.persistence.converter.KnowledgeBaseConverter;
import com.enterprise.rag.infrastructure.persistence.entity.KnowledgeBasePO;
import com.enterprise.rag.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 知识库仓储实现
 */
@Repository
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {
    
    private final KnowledgeBaseMapper mapper;
    private final KnowledgeBaseConverter converter;
    
    public KnowledgeBaseRepositoryImpl(KnowledgeBaseMapper mapper, KnowledgeBaseConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }
    
    @Override
    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        KnowledgeBasePO entity = converter.toEntity(knowledgeBase);
        
        if (mapper.selectById(knowledgeBase.getId()) != null) {
            mapper.updateById(entity);
        } else {
            mapper.insert(entity);
        }
        
        return converter.toDomain(mapper.selectById(entity.getId()));
    }
    
    @Override
    public Optional<KnowledgeBase> findById(String id) {
        KnowledgeBasePO entity = mapper.selectById(id);
        return Optional.ofNullable(entity).map(converter::toDomain);
    }
    
    @Override
    public Optional<KnowledgeBase> findByIndexName(String indexName) {
        return mapper.findByIndexName(indexName).map(converter::toDomain);
    }
    
    @Override
    public List<KnowledgeBase> findByTenantId(String tenantId) {
        return mapper.findByTenantId(tenantId).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(String id) {
        mapper.deleteById(id);
    }
    
    @Override
    public boolean existsById(String id) {
        return mapper.selectById(id) != null;
    }
}
