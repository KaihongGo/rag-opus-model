package com.enterprise.rag.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.infrastructure.persistence.entity.KnowledgeBasePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 知识库 Mapper 接口
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBasePO> {
    
    @Select("SELECT * FROM knowledge_base WHERE index_name = #{indexName} AND deleted = 0")
    Optional<KnowledgeBasePO> findByIndexName(@Param("indexName") String indexName);
    
    @Select("SELECT * FROM knowledge_base WHERE tenant_id = #{tenantId} AND deleted = 0")
    List<KnowledgeBasePO> findByTenantId(@Param("tenantId") String tenantId);
}
