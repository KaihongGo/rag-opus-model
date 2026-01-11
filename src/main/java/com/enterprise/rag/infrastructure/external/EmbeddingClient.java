package com.enterprise.rag.infrastructure.external;

import com.enterprise.rag.domain.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Embedding 客户端实现
 * 
 * <p>调用外部 Embedding 服务生成向量</p>
 */
@Slf4j
@Service
public class EmbeddingClient implements EmbeddingService {
    
    private final RestTemplate restTemplate;
    
    @Value("${embedding.service.base-url:http://localhost:8081}")
    private String baseUrl;
    
    @Value("${embedding.service.default-model:text-embedding-ada-002}")
    private String defaultModel;
    
    @Value("${embedding.service.default-dimension:1536}")
    private int defaultDimension;
    
    private final Map<String, Integer> modelDimensions = Map.of(
            "text-embedding-ada-002", 1536,
            "text-embedding-3-small", 1536,
            "text-embedding-3-large", 3072,
            "bge-large-zh", 1024,
            "bge-base-zh", 768
    );
    
    public EmbeddingClient() {
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public float[] embed(String text, String modelId) {
        List<float[]> results = embedBatch(List.of(text), modelId);
        return results.isEmpty() ? new float[0] : results.get(0);
    }
    
    @Override
    public List<float[]> embedBatch(List<String> texts, String modelId) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        
        String model = modelId != null ? modelId : defaultModel;
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "input", texts
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/v1/embeddings",
                    HttpMethod.POST,
                    request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseEmbeddingResponse(response.getBody());
            }
            
            log.error("Embedding service returned non-success status: {}", response.getStatusCode());
            return generateMockEmbeddings(texts.size(), getDimension(model));
            
        } catch (Exception e) {
            log.warn("Failed to call embedding service, using mock embeddings: {}", e.getMessage());
            return generateMockEmbeddings(texts.size(), getDimension(model));
        }
    }
    
    @Override
    public int getDimension(String modelId) {
        String model = modelId != null ? modelId : defaultModel;
        return modelDimensions.getOrDefault(model, defaultDimension);
    }
    
    @SuppressWarnings("unchecked")
    private List<float[]> parseEmbeddingResponse(Map<String, Object> responseBody) {
        List<float[]> embeddings = new ArrayList<>();
        
        Object dataObj = responseBody.get("data");
        if (dataObj instanceof List) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
            
            for (Map<String, Object> item : dataList) {
                Object embeddingObj = item.get("embedding");
                if (embeddingObj instanceof List) {
                    List<Number> embeddingList = (List<Number>) embeddingObj;
                    float[] embedding = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        embedding[i] = embeddingList.get(i).floatValue();
                    }
                    embeddings.add(embedding);
                }
            }
        }
        
        return embeddings;
    }
    
    /**
     * 生成模拟向量 (用于测试或服务不可用时的降级)
     */
    private List<float[]> generateMockEmbeddings(int count, int dimension) {
        List<float[]> embeddings = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            float[] embedding = new float[dimension];
            float norm = 0;
            
            for (int j = 0; j < dimension; j++) {
                embedding[j] = (float) random.nextGaussian();
                norm += embedding[j] * embedding[j];
            }
            
            // 归一化
            norm = (float) Math.sqrt(norm);
            for (int j = 0; j < dimension; j++) {
                embedding[j] /= norm;
            }
            
            embeddings.add(embedding);
        }
        
        return embeddings;
    }
}
