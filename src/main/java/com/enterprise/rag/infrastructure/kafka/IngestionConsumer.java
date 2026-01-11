package com.enterprise.rag.infrastructure.kafka;

import com.enterprise.rag.application.service.IngestionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 数据摄取 Kafka 消费者
 * 
 * <p>监听 rag.ingestion.raw 主题，处理入站数据</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionConsumer {
    
    private final ObjectMapper objectMapper;
    private final IngestionService ingestionService;
    
    @KafkaListener(
            topics = "${rag.ingestion.topic:rag.ingestion.raw}",
            groupId = "${spring.kafka.consumer.group-id:rag-ingestion-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String messageKey = record.key();
        String messageValue = record.value();
        
        log.debug("Received ingestion message: key={}, partition={}, offset={}", 
                messageKey, record.partition(), record.offset());
        
        try {
            // 解析消息
            IngestionMessage message = objectMapper.readValue(messageValue, IngestionMessage.class);
            
            // 处理消息
            processMessage(message);
            
            // 确认消息
            acknowledgment.acknowledge();
            log.debug("Successfully processed ingestion message: {}", messageKey);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse ingestion message: {}", e.getMessage());
            // 解析失败的消息直接确认，避免重复消费
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process ingestion message: {}", e.getMessage(), e);
            // 处理失败，不确认，让 Kafka 重试
            // 实际生产环境应该实现更完善的重试和死信队列机制
            throw new RuntimeException("Ingestion processing failed", e);
        }
    }
    
    private void processMessage(IngestionMessage message) {
        switch (message.getOperation()) {
            case INDEX -> ingestionService.ingestDocument(
                    message.getKnowledgeBaseId(),
                    message.getDocumentId(),
                    message.getContent(),
                    message.getMetadata(),
                    message.getSourceDocumentId(),
                    message.getChunkIndex()
            );
            case DELETE -> ingestionService.deleteDocument(
                    message.getKnowledgeBaseId(),
                    message.getDocumentId()
            );
            case UPDATE -> ingestionService.updateDocument(
                    message.getKnowledgeBaseId(),
                    message.getDocumentId(),
                    message.getContent(),
                    message.getMetadata()
            );
        }
    }
}
