package com.enterprise.rag.interfaces.rest;

import com.enterprise.rag.application.service.SearchService;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import com.enterprise.rag.interfaces.rest.assembler.DtoAssembler;
import com.enterprise.rag.interfaces.rest.dto.KnowledgeBaseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库管理 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    
    private final SearchService searchService;
    private final DtoAssembler dtoAssembler;
    
    /**
     * 创建知识库
     * 
     * POST /api/v1/knowledge-bases
     */
    @PostMapping
    public ResponseEntity<KnowledgeBaseDTO> createKnowledgeBase(
            @Valid @RequestBody KnowledgeBaseDTO requestDTO) {
        
        log.info("Creating knowledge base: {}", requestDTO.getName());
        
        KnowledgeBase knowledgeBase = dtoAssembler.toKnowledgeBase(requestDTO);
        KnowledgeBase created = searchService.createKnowledgeBase(knowledgeBase);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dtoAssembler.toKnowledgeBaseDTO(created));
    }
    
    /**
     * 获取知识库详情
     * 
     * GET /api/v1/knowledge-bases/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBaseDTO> getKnowledgeBase(@PathVariable String id) {
        log.debug("Getting knowledge base: {}", id);
        
        KnowledgeBase knowledgeBase = searchService.getKnowledgeBase(id);
        return ResponseEntity.ok(dtoAssembler.toKnowledgeBaseDTO(knowledgeBase));
    }
    
    /**
     * 获取租户下的所有知识库
     * 
     * GET /api/v1/knowledge-bases?tenantId=xxx
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeBaseDTO>> listKnowledgeBases(
            @RequestParam String tenantId) {
        
        log.debug("Listing knowledge bases for tenant: {}", tenantId);
        
        List<KnowledgeBase> knowledgeBases = searchService.listKnowledgeBases(tenantId);
        List<KnowledgeBaseDTO> dtos = knowledgeBases.stream()
                .map(dtoAssembler::toKnowledgeBaseDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * 删除知识库
     * 
     * DELETE /api/v1/knowledge-bases/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledgeBase(@PathVariable String id) {
        log.info("Deleting knowledge base: {}", id);
        
        searchService.deleteKnowledgeBase(id);
        return ResponseEntity.noContent().build();
    }
}
