package com.enterprise.rag.interfaces.rest;

import com.enterprise.rag.application.service.SearchService;
import com.enterprise.rag.domain.model.search.SearchRequest;
import com.enterprise.rag.domain.model.search.SearchResult;
import com.enterprise.rag.interfaces.rest.assembler.DtoAssembler;
import com.enterprise.rag.interfaces.rest.dto.SearchRequestDTO;
import com.enterprise.rag.interfaces.rest.dto.SearchResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    private final DtoAssembler dtoAssembler;
    
    /**
     * 执行混合检索
     * 
     * POST /api/v1/search
     */
    @PostMapping
    public ResponseEntity<SearchResponseDTO> search(@Valid @RequestBody SearchRequestDTO requestDTO) {
        log.info("Search request received: kb={}, query={}, methods={}",
                requestDTO.getKnowledgeBaseId(),
                requestDTO.getQueryText().substring(0, Math.min(50, requestDTO.getQueryText().length())),
                requestDTO.getRetrievalMethods().size());
        
        // 转换为领域模型
        SearchRequest request = dtoAssembler.toSearchRequest(requestDTO);
        
        // 执行搜索
        SearchResult result = searchService.search(request);
        
        // 转换为 DTO
        SearchResponseDTO response = dtoAssembler.toSearchResponseDTO(result);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 简化的向量检索接口
     * 
     * GET /api/v1/search/vector
     */
    @GetMapping("/vector")
    public ResponseEntity<SearchResponseDTO> vectorSearch(
            @RequestParam String knowledgeBaseId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") Integer topK) {
        
        SearchRequestDTO requestDTO = new SearchRequestDTO();
        requestDTO.setKnowledgeBaseId(knowledgeBaseId);
        requestDTO.setQueryText(query);
        requestDTO.setTopK(topK);
        
        SearchRequestDTO.RetrievalMethodDTO vectorMethod = new SearchRequestDTO.RetrievalMethodDTO();
        vectorMethod.setType(com.enterprise.rag.domain.model.search.RetrievalMethodType.VECTOR);
        requestDTO.setRetrievalMethods(java.util.List.of(vectorMethod));
        requestDTO.setFusionStrategy(com.enterprise.rag.domain.model.search.FusionStrategyType.NONE);
        
        return search(requestDTO);
    }
    
    /**
     * 简化的文本检索接口
     * 
     * GET /api/v1/search/text
     */
    @GetMapping("/text")
    public ResponseEntity<SearchResponseDTO> textSearch(
            @RequestParam String knowledgeBaseId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") Integer topK) {
        
        SearchRequestDTO requestDTO = new SearchRequestDTO();
        requestDTO.setKnowledgeBaseId(knowledgeBaseId);
        requestDTO.setQueryText(query);
        requestDTO.setTopK(topK);
        
        SearchRequestDTO.RetrievalMethodDTO textMethod = new SearchRequestDTO.RetrievalMethodDTO();
        textMethod.setType(com.enterprise.rag.domain.model.search.RetrievalMethodType.TEXT);
        requestDTO.setRetrievalMethods(java.util.List.of(textMethod));
        requestDTO.setFusionStrategy(com.enterprise.rag.domain.model.search.FusionStrategyType.NONE);
        
        return search(requestDTO);
    }
    
    /**
     * 简化的混合检索接口
     * 
     * GET /api/v1/search/hybrid
     */
    @GetMapping("/hybrid")
    public ResponseEntity<SearchResponseDTO> hybridSearch(
            @RequestParam String knowledgeBaseId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") Integer topK,
            @RequestParam(defaultValue = "0.5") Double vectorWeight,
            @RequestParam(defaultValue = "0.5") Double textWeight,
            @RequestParam(defaultValue = "RRF") String fusionStrategy) {
        
        SearchRequestDTO requestDTO = new SearchRequestDTO();
        requestDTO.setKnowledgeBaseId(knowledgeBaseId);
        requestDTO.setQueryText(query);
        requestDTO.setTopK(topK);
        
        // 向量检索方法
        SearchRequestDTO.RetrievalMethodDTO vectorMethod = new SearchRequestDTO.RetrievalMethodDTO();
        vectorMethod.setType(com.enterprise.rag.domain.model.search.RetrievalMethodType.VECTOR);
        vectorMethod.setWeight(vectorWeight);
        
        // 文本检索方法
        SearchRequestDTO.RetrievalMethodDTO textMethod = new SearchRequestDTO.RetrievalMethodDTO();
        textMethod.setType(com.enterprise.rag.domain.model.search.RetrievalMethodType.TEXT);
        textMethod.setWeight(textWeight);
        
        requestDTO.setRetrievalMethods(java.util.List.of(vectorMethod, textMethod));
        
        // 设置融合策略
        requestDTO.setFusionStrategy(
                "LINEAR".equalsIgnoreCase(fusionStrategy) 
                        ? com.enterprise.rag.domain.model.search.FusionStrategyType.LINEAR_WEIGHT
                        : com.enterprise.rag.domain.model.search.FusionStrategyType.RRF
        );
        
        return search(requestDTO);
    }
}
