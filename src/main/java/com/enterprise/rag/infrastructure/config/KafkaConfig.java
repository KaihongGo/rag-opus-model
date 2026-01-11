package com.enterprise.rag.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 配置类
 */
@Configuration
public class KafkaConfig {
    
    @Value("${rag.ingestion.topic:rag.ingestion.raw}")
    private String ingestionTopic;
    
    @Bean
    public NewTopic ingestionTopic() {
        return TopicBuilder.name(ingestionTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic ingestionDlqTopic() {
        return TopicBuilder.name(ingestionTopic + ".dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
