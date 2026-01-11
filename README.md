# RAG Retrieval Platform - 元数据驱动的企业级 RAG 检索中台

## 项目概述

基于 DDD 架构设计的企业级 RAG (Retrieval-Augmented Generation) 检索平台，支持：

- **元数据驱动**：动态定义知识库字段，无需硬编码
- **混合检索**：支持向量检索、文本检索及混合检索
- **多种融合策略**：Linear Weighting 和 RRF (Reciprocal Rank Fusion)
- **策略模式**：可扩展的检索和融合策略

## 技术栈

- **Java 17** + **Spring Boot 3.x**
- **PostgreSQL** - 知识库元数据存储
- **Elasticsearch 8.x** - 文档索引和检索（使用 Java API Client）
- **Kafka** - 异步数据摄取
- **MyBatis-Plus** - 数据持久化

## 项目结构 (DDD 分层)

```
src/main/java/com/enterprise/rag/
├── RagRetrievalPlatformApplication.java    # 启动类
│
├── domain/                                  # 领域层
│   ├── model/                              # 领域模型
│   │   ├── knowledgebase/                  # 知识库聚合
│   │   │   ├── KnowledgeBase.java          # 聚合根
│   │   │   ├── FieldDefinition.java        # 字段定义（值对象）
│   │   │   ├── EmbeddingConfig.java        # 嵌入配置（值对象）
│   │   │   ├── IndexType.java              # 索引类型枚举
│   │   │   ├── KnowledgeBaseStatus.java    # 状态枚举
│   │   │   └── KnowledgeBaseRepository.java # 仓储接口
│   │   ├── document/                       # 文档实体
│   │   │   └── Document.java
│   │   └── search/                         # 搜索值对象
│   │       ├── SearchRequest.java
│   │       ├── SearchResult.java
│   │       ├── SearchResultItem.java
│   │       ├── RetrievalMethod.java
│   │       ├── RetrievalMethodType.java
│   │       └── FusionStrategyType.java
│   └── service/                            # 领域服务
│       ├── EmbeddingService.java           # 嵌入服务接口
│       ├── search/                         # 检索策略
│       │   ├── SearchStrategy.java         # 策略接口
│       │   ├── VectorSearchStrategy.java   # 向量检索实现
│       │   ├── TextSearchStrategy.java     # 文本检索实现
│       │   └── SearchStrategyFactory.java  # 策略工厂
│       └── fusion/                         # 融合策略
│           ├── FusionStrategy.java         # 融合接口
│           ├── LinearWeightFusionStrategy.java  # 线性加权
│           ├── RRFFusionStrategy.java      # RRF 融合
│           ├── FusionStrategyFactory.java  # 融合工厂
│           └── FusionProcessor.java        # 融合处理器
│
├── application/                            # 应用层
│   └── service/
│       ├── SearchService.java              # 搜索应用服务
│       └── IngestionService.java           # 摄取应用服务
│
├── infrastructure/                         # 基础设施层
│   ├── config/                             # 配置
│   │   ├── ElasticsearchConfig.java
│   │   ├── MyBatisPlusConfig.java
│   │   └── KafkaConfig.java
│   ├── persistence/                        # 持久化
│   │   ├── entity/
│   │   │   └── KnowledgeBasePO.java
│   │   ├── mapper/
│   │   │   └── KnowledgeBaseMapper.java
│   │   ├── converter/
│   │   │   └── KnowledgeBaseConverter.java
│   │   └── repository/
│   │       └── KnowledgeBaseRepositoryImpl.java
│   ├── elasticsearch/                      # ES 操作
│   │   ├── ElasticsearchIndexManager.java
│   │   └── ElasticsearchDocumentClient.java
│   ├── kafka/                              # Kafka 消费
│   │   ├── IngestionConsumer.java
│   │   └── IngestionMessage.java
│   └── external/                           # 外部服务
│       └── EmbeddingClient.java
│
└── interfaces/                             # 接口层
    └── rest/                               # REST API
        ├── SearchController.java
        ├── KnowledgeBaseController.java
        ├── GlobalExceptionHandler.java
        ├── dto/
        │   ├── SearchRequestDTO.java
        │   ├── SearchResponseDTO.java
        │   └── KnowledgeBaseDTO.java
        └── assembler/
            └── DtoAssembler.java
```

## 快速开始

### 1. 启动基础设施

```bash
# 启动 PostgreSQL, Elasticsearch, Kafka
docker-compose up -d

# 等待服务就绪后初始化 ES
cd elasticsearch
chmod +x init-es.sh
./init-es.sh
```

### 2. 启动应用

```bash
mvn spring-boot:run
```

### 3. 创建知识库

```bash
curl -X POST http://localhost:8080/api/v1/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Product Documentation",
    "description": "Product docs knowledge base",
    "tenantId": "tenant-001",
    "fieldDefinitions": [
      {
        "fieldName": "content",
        "indexType": "VECTOR",
        "isFilter": false,
        "required": true,
        "embeddingConfig": {
          "modelId": "text-embedding-ada-002",
          "dimension": 1536,
          "similarity": "cosine"
        }
      },
      {
        "fieldName": "title",
        "indexType": "TEXT",
        "isFilter": true,
        "required": true
      },
      {
        "fieldName": "category",
        "indexType": "KEYWORD",
        "isFilter": true,
        "required": false
      }
    ]
  }'
```

### 4. 发送数据到 Kafka

```bash
# 发送摄取消息
echo '{
  "knowledgeBaseId": "demo-kb-001",
  "content": {
    "content": "This is the main content for vector search",
    "title": "Sample Document",
    "category": "demo"
  },
  "metadata": {
    "source": "manual",
    "author": "system"
  },
  "operation": "INDEX"
}' | kafka-console-producer --broker-list localhost:9092 --topic rag.ingestion.raw
```

### 5. 执行检索

#### 混合检索 (RRF 融合)

```bash
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "knowledgeBaseId": "demo-kb-001",
    "queryText": "RAG retrieval system",
    "retrievalMethods": [
      {
        "type": "VECTOR",
        "weight": 0.7,
        "vectorConfig": {
          "vectorField": "content",
          "numCandidates": 100
        }
      },
      {
        "type": "TEXT",
        "weight": 0.3,
        "targetFields": ["title", "content"]
      }
    ],
    "fusionStrategy": "RRF",
    "rrfK": 60,
    "topK": 10
  }'
```

#### 简化接口

```bash
# 向量检索
curl "http://localhost:8080/api/v1/search/vector?knowledgeBaseId=demo-kb-001&query=RAG+system&topK=10"

# 文本检索
curl "http://localhost:8080/api/v1/search/text?knowledgeBaseId=demo-kb-001&query=RAG+system&topK=10"

# 混合检索
curl "http://localhost:8080/api/v1/search/hybrid?knowledgeBaseId=demo-kb-001&query=RAG+system&topK=10&vectorWeight=0.6&textWeight=0.4&fusionStrategy=RRF"
```

## 核心设计

### 策略模式 - 检索策略

```java
public interface SearchStrategy {
    RetrievalMethodType getSupportedType();
    
    SearchRequest.Builder buildSearchRequest(
        KnowledgeBase knowledgeBase,
        String queryText,
        RetrievalMethod retrievalMethod,
        float[] queryVector,
        int topK
    );
}
```

实现类：
- `VectorSearchStrategy` - 使用 ES 8.x knn 查询
- `TextSearchStrategy` - 使用 ES match/multi_match 查询

### 策略模式 - 融合策略

```java
public interface FusionStrategy {
    FusionStrategyType getType();
    
    List<SearchResultItem> fuse(
        Map<RetrievalMethodType, List<RankedDocument>> resultsByMethod,
        Map<RetrievalMethodType, Double> weights,
        int topK
    );
}
```

实现类：
- `LinearWeightFusionStrategy` - 归一化分数线性加权
- `RRFFusionStrategy` - Reciprocal Rank Fusion

### RRF 算法

```
RRF_score(d) = Σ 1 / (k + rank_i(d))

其中:
- k: 常数参数（默认 60）
- rank_i(d): 文档 d 在第 i 个检索方法中的排名
```

### 动态 Mapping

系统根据 `KnowledgeBase.FieldDefinitions` 动态生成 ES mapping：

| IndexType | ES 类型 | 用途 |
|-----------|--------|------|
| VECTOR | text + dense_vector | 存储原文 + 向量 |
| TEXT | text (with keyword) | 全文搜索 |
| KEYWORD | keyword | 精确匹配/过滤 |

## ES Index Template

通过 Index Template 支持动态知识库：

```json
{
  "index_patterns": ["rag_kb_*"],
  "template": {
    "mappings": {
      "dynamic_templates": [
        {
          "vectors": {
            "match": "*_vector",
            "mapping": {
              "type": "dense_vector",
              "dims": 1536,
              "index": true,
              "similarity": "cosine"
            }
          }
        }
      ]
    }
  }
}
```

## 扩展指南

### 添加新的检索策略

1. 实现 `SearchStrategy` 接口
2. 在 `RetrievalMethodType` 中添加新类型
3. 策略会自动注册到 `SearchStrategyFactory`

### 添加新的融合策略

1. 实现 `FusionStrategy` 接口
2. 在 `FusionStrategyType` 中添加新类型
3. 策略会自动注册到 `FusionStrategyFactory`

## 监控端点

- 健康检查: `GET /actuator/health`
- 指标: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

## 许可证

MIT License
