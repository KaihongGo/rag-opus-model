# Elasticsearch Mapping 设计文档

## 1. 概述

本文档详细介绍 RAG 检索平台中 Elasticsearch 索引的 Mapping 设计。系统采用**元数据驱动**的动态 Mapping 策略，通过 Index Template 和动态字段生成，实现灵活的知识库结构定义。

---

## 2. Index Template 设计

### 2.1 模板定义

位置: [elasticsearch/index-template.json](../elasticsearch/index-template.json)

```json
{
  "index_patterns": ["rag_kb_*"],
  "priority": 100,
  "template": {
    "settings": { ... },
    "mappings": { ... }
  }
}
```

**核心配置**:
- `index_patterns`: 匹配所有 `rag_kb_*` 前缀的索引
- `priority`: 优先级 100，确保优先应用此模板
- 所有知识库索引自动继承此模板的配置

### 2.2 Settings 配置

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "rag_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "asciifolding", "snowball"]
        },
        "rag_cjk_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "cjk_width", "cjk_bigram"]
        }
      }
    }
  }
}
```

#### 分片配置

| 参数 | 值 | 说明 |
|------|-----|------|
| `number_of_shards` | 1 | 主分片数量，适合中小规模数据 |
| `number_of_replicas` | 0 | 副本数量，开发环境无需副本 |

**生产环境建议**:
- `number_of_shards`: 根据数据量调整 (每分片 20-50GB)
- `number_of_replicas`: 至少设为 1，保证高可用性

#### 分析器配置

**rag_analyzer** (英文分析器):
- `tokenizer: standard`: 标准分词器
- `filter`:
  - `lowercase`: 转小写
  - `asciifolding`: 将重音字符转换为 ASCII
  - `snowball`: 词干提取 (如 running → run)

**rag_cjk_analyzer** (中日韩分析器):
- `tokenizer: standard`: 标准分词器
- `filter`:
  - `lowercase`: 转小写
  - `cjk_width`: 全角半角转换
  - `cjk_bigram`: 二元切分 (如 "搜索引擎" → ["搜索", "索引", "引擎"])

**使用场景**: 可在 `FieldDefinition` 中指定使用哪个分析器。

---

## 3. Mappings 设计

### 3.1 Dynamic Templates (动态模板)

ES 的 Dynamic Templates 允许对未定义的字段进行模式匹配并自动应用 Mapping。

#### 3.1.1 向量字段模板

```json
{
  "vectors": {
    "match": "*_vector",
    "mapping": {
      "type": "dense_vector",
      "dims": 1536,
      "index": true,
      "similarity": "cosine",
      "index_options": {
        "type": "hnsw",
        "m": 16,
        "ef_construction": 100
      }
    }
  }
}
```

**规则**: 所有以 `_vector` 结尾的字段自动映射为 `dense_vector` 类型。

**字段配置解析**:

| 参数 | 值 | 说明 |
|------|-----|------|
| `type` | `dense_vector` | 密集向量类型 |
| `dims` | 1536 | 向量维度 (OpenAI text-embedding-ada-002 的维度) |
| `index` | `true` | 启用索引，支持 kNN 查询 |
| `similarity` | `cosine` | 相似度算法 (余弦相似度) |

**HNSW 索引参数**:

| 参数 | 值 | 说明 |
|------|-----|------|
| `type` | `hnsw` | 使用 HNSW 算法 (Hierarchical Navigable Small World) |
| `m` | 16 | 每个节点的最大连接数，越大查询越准确但索引越大 |
| `ef_construction` | 100 | 构建索引时的搜索范围，越大构建慢但查询准 |

**性能调优建议**:
- **高精度场景**: `m=32, ef_construction=200`
- **平衡场景**: `m=16, ef_construction=100` (默认)
- **高性能场景**: `m=8, ef_construction=50`

#### 3.1.2 字符串字段模板

```json
{
  "strings_as_text": {
    "match_mapping_type": "string",
    "mapping": {
      "type": "text",
      "analyzer": "standard",
      "fields": {
        "keyword": {
          "type": "keyword",
          "ignore_above": 256
        }
      }
    }
  }
}
```

**规则**: 所有未定义的字符串字段自动映射为 `text` 类型，并附带 `keyword` 子字段。

**Multi-Field 设计**:
- **主字段 (text)**: 用于全文检索，支持分词
- **子字段 (keyword)**: 用于精确匹配、聚合、排序

**使用示例**:
```json
// 全文检索
{ "match": { "title": "搜索引擎" } }

// 精确匹配
{ "term": { "title.keyword": "Elasticsearch 入门" } }

// 聚合
{ "terms": { "field": "category.keyword" } }
```

### 3.2 系统字段 (Static Fields)

系统预定义了一组标准字段，用于文档管理和追踪。

```json
{
  "properties": {
    "_doc_id": {
      "type": "keyword"
    },
    "_knowledge_base_id": {
      "type": "keyword"
    },
    "_created_at": {
      "type": "date"
    },
    "_updated_at": {
      "type": "date"
    },
    "_source_document_id": {
      "type": "keyword"
    },
    "_chunk_index": {
      "type": "integer"
    },
    "_metadata": {
      "type": "object",
      "enabled": true
    }
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `_doc_id` | keyword | 文档唯一标识符 |
| `_knowledge_base_id` | keyword | 所属知识库 ID，用于多租户隔离 |
| `_created_at` | date | 文档创建时间 |
| `_updated_at` | date | 文档更新时间 |
| `_source_document_id` | keyword | 源文档 ID (用于关联分块前的原始文档) |
| `_chunk_index` | integer | 分块索引 (如一个长文档分成多个 chunk) |
| `_metadata` | object | 自定义元数据，存储额外的业务信息 |

**命名约定**: 系统字段以 `_` 开头，避免与业务字段冲突。

---

## 4. 动态 Mapping 生成

### 4.1 生成流程

系统在创建知识库时，根据 `FieldDefinition` 动态生成 ES Mapping。

**代码位置**: [ElasticsearchIndexManager.java](../src/main/java/com/enterprise/rag/infrastructure/elasticsearch/ElasticsearchIndexManager.java)

```java
private Map<String, Property> buildProperties(KnowledgeBase knowledgeBase) {
    Map<String, Property> properties = new HashMap<>();
    
    // 1. 添加系统字段
    properties.put("_doc_id", Property.of(p -> p.keyword(k -> k)));
    properties.put("_knowledge_base_id", Property.of(p -> p.keyword(k -> k)));
    // ...
    
    // 2. 根据字段定义动态构建
    for (FieldDefinition fd : knowledgeBase.getFieldDefinitionList()) {
        Property property = buildPropertyFromDefinition(fd);
        properties.put(fd.getFieldName(), property);
        
        // 3. 向量字段额外创建 _vector 字段
        if (fd.isVectorField()) {
            Property vectorProperty = buildDenseVectorProperty(fd);
            properties.put(fd.getVectorFieldName(), vectorProperty);
        }
    }
    
    return properties;
}
```

### 4.2 字段类型映射

| FieldDefinition.IndexType | ES Mapping Type | 说明 |
|---------------------------|-----------------|------|
| `VECTOR` | `text` + `dense_vector` | 原文 + 向量两个字段 |
| `TEXT` | `text` (with keyword) | 全文检索 + 精确匹配 |
| `KEYWORD` | `keyword` | 精确匹配、聚合、排序 |

### 4.3 向量字段特殊处理

**设计原理**: 向量字段需要同时存储原文和向量。

**示例**: 
- 用户定义字段: `content` (类型: VECTOR)
- 生成 ES 字段:
  - `content`: text 类型，存储原文
  - `content_vector`: dense_vector 类型，存储向量

**Java 代码实现**:

```java
private Property buildDenseVectorProperty(FieldDefinition fd) {
    int dimension = fd.getEmbeddingConfig().getDimension();
    String similarity = fd.getEmbeddingConfig().getSimilarity();
    
    return Property.of(p -> p
        .denseVector(dv -> dv
            .dims(dimension)
            .index(true)
            .similarity(similarity)
            .indexOptions(io -> io
                .type("hnsw")
                .m(16)
                .efConstruction(100)
            )
        ));
}
```

**向量配置从 `EmbeddingConfig` 中读取**:
- `dimension`: 向量维度 (如 1536, 768, 512)
- `similarity`: 相似度算法 (cosine, dot_product, l2_norm)

---

## 5. 索引命名规则

### 5.1 命名格式

```
rag_kb_{tenantId}_{knowledgeBaseId}
```

**示例**:
```
rag_kb_tenant001_kb123456
```

### 5.2 命名优势

1. **多租户隔离**: 通过 `tenantId` 实现物理隔离
2. **易于管理**: 通过前缀 `rag_kb_` 统一管理所有知识库索引
3. **模式匹配**: 使用 Index Template 的 `rag_kb_*` 模式统一配置

### 5.3 代码实现

```java
// KnowledgeBase.java
private String generateIndexName(String tenantId, String knowledgeBaseId) {
    return String.format("rag_kb_%s_%s", tenantId, knowledgeBaseId);
}
```

---

## 6. 实际案例

### 6.1 案例: 技术文档知识库

**需求**: 存储技术文档，支持标题、内容的语义检索，以及按分类、标签过滤。

**字段定义**:

```java
KnowledgeBase kb = KnowledgeBase.builder()
    .name("技术文档库")
    .tenantId("tenant001")
    .fieldDefinitions(List.of(
        // 标题 - 文本检索
        FieldDefinition.textField("title", false),
        
        // 内容 - 向量检索
        FieldDefinition.vectorField("content", 
            EmbeddingConfig.builder()
                .model("text-embedding-ada-002")
                .dimension(1536)
                .similarity("cosine")
                .build()),
        
        // 分类 - 精确过滤
        FieldDefinition.keywordField("category", true),
        
        // 标签 - 精确过滤
        FieldDefinition.keywordField("tags", true)
    ))
    .build();
```

**生成的 ES Mapping**:

```json
{
  "mappings": {
    "properties": {
      "_doc_id": { "type": "keyword" },
      "_knowledge_base_id": { "type": "keyword" },
      "_created_at": { "type": "date" },
      
      "title": {
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "content": {
        "type": "text"
      },
      "content_vector": {
        "type": "dense_vector",
        "dims": 1536,
        "index": true,
        "similarity": "cosine",
        "index_options": {
          "type": "hnsw",
          "m": 16,
          "ef_construction": 100
        }
      },
      "category": {
        "type": "keyword"
      },
      "tags": {
        "type": "keyword"
      }
    }
  }
}
```

**检索示例**:

```java
SearchRequest request = SearchRequest.builder()
    .knowledgeBaseId(kb.getId())
    .queryText("如何优化 Elasticsearch 性能")
    .retrievalMethods(List.of(
        // 向量检索 content 字段
        RetrievalMethod.builder()
            .type(RetrievalMethodType.VECTOR)
            .targetFields(List.of("content"))
            .weight(0.7)
            .build(),
        // 文本检索 title 字段
        RetrievalMethod.builder()
            .type(RetrievalMethodType.TEXT)
            .targetFields(List.of("title"))
            .weight(0.3)
            .build()
    ))
    .filters(Map.of("category", "Elasticsearch"))
    .fusionStrategy(FusionStrategyType.RRF)
    .topK(10)
    .build();
```

---

## 7. 相似度算法选择

ES 8.x 支持三种向量相似度算法：

### 7.1 Cosine Similarity (余弦相似度)

```
similarity = (A · B) / (||A|| × ||B||)
```

**值域**: [-1, 1]，1 表示完全相同，-1 表示完全相反

**适用场景**:
- 文本向量 (如 OpenAI embeddings)
- 关注方向而非大小
- **推荐使用**

### 7.2 Dot Product (点积)

```
similarity = A · B
```

**值域**: (-∞, +∞)

**适用场景**:
- 向量已归一化
- 需要考虑向量长度
- 某些图像向量

### 7.3 L2 Norm (欧几里得距离)

```
distance = ||A - B||
```

**值域**: [0, +∞)，0 表示完全相同

**适用场景**:
- 需要绝对距离的场景
- 图像向量
- **注意**: ES 返回的是距离而非相似度

### 7.4 选择建议

| 向量来源 | 推荐算法 | 原因 |
|---------|---------|------|
| OpenAI (text-embedding-*) | cosine | 向量已归一化，余弦效果最佳 |
| Sentence-BERT | cosine | 语义相似度场景 |
| 图像向量 (ResNet) | l2_norm | 保留距离信息 |
| 自定义向量 (未归一化) | dot_product | 考虑向量大小 |

---

## 8. 性能优化

### 8.1 HNSW 参数调优

**查询速度 vs 召回率权衡**:

```
高召回率 (慢):  m=32, ef_construction=200
平衡:          m=16, ef_construction=100  ← 默认
高速度 (低召回): m=8,  ef_construction=50
```

**查询时参数** (`num_candidates`):
```java
// VectorSearchStrategy.java
int numCandidates = topK * 10;  // 建议 10-20 倍
```

### 8.2 分片策略

**数据量 vs 分片数**:

| 数据量 | 建议分片数 | 说明 |
|--------|----------|------|
| < 1GB | 1 | 单分片足够 |
| 1GB - 50GB | 1-3 | 根据节点数调整 |
| 50GB - 500GB | 3-10 | 每分片 50GB 左右 |
| > 500GB | 10+ | 考虑使用别名和滚动索引 |

**计算公式**:
```
分片数 = 总数据量 (GB) / 50GB
```

### 8.3 索引刷新策略

**开发环境** (默认):
```json
{
  "refresh_interval": "1s"
}
```

**生产环境** (优化写入):
```json
{
  "refresh_interval": "30s"
}
```

**大批量导入时**:
```java
// 临时禁用刷新
esClient.indices().putSettings(s -> s
    .index(indexName)
    .settings(settings -> settings.refreshInterval(t -> t.time("-1")))
);

// 导入数据...

// 恢复并手动刷新
esClient.indices().refresh(r -> r.index(indexName));
```

---

## 9. Mapping 更新策略

### 9.1 可更新的操作

✅ **允许的操作**:
- 添加新字段
- 修改 `ignore_above` 参数
- 添加 multi-field
- 更新 `dynamic` 设置

### 9.2 不可更新的操作

❌ **不允许的操作**:
- 修改字段类型 (如 text → keyword)
- 修改 analyzer
- 修改向量维度 (`dims`)
- 删除字段

**原因**: ES 底层已建立倒排索引，修改类型会导致数据不一致。

### 9.3 需要 Reindex 的场景

如果需要修改不可变属性，需要 Reindex：

```java
// 1. 创建新索引
esClient.indices().create(c -> c.index(newIndexName));

// 2. Reindex 数据
esClient.reindex(r -> r
    .source(s -> s.index(oldIndexName))
    .dest(d -> d.index(newIndexName))
);

// 3. 使用别名切换
esClient.indices().updateAliases(a -> a
    .actions(action -> action
        .remove(remove -> remove.index(oldIndexName).alias(aliasName))
        .add(add -> add.index(newIndexName).alias(aliasName))
    )
);

// 4. 删除旧索引
esClient.indices().delete(d -> d.index(oldIndexName));
```

---

## 10. 常见问题

### Q1: 为什么向量字段需要两个 ES 字段？

**A**: 
- `content`: 存储原文，用于展示和文本检索
- `content_vector`: 存储向量，用于 kNN 检索
- 分离存储避免向量字段占用文本检索空间

### Q2: 如何选择 `m` 和 `ef_construction` 参数？

**A**: 
- 数据量 < 10万: 默认即可 (`m=16, ef=100`)
- 数据量 > 100万: 适当增大 (`m=32, ef=200`)
- 对召回率要求极高: `m=64, ef=500` (构建慢)

### Q3: 动态模板会影响性能吗？

**A**: 
- 动态模板只在字段**首次出现**时触发
- 后续文档直接使用已生成的 Mapping
- 性能影响可忽略

### Q4: 如何支持多语言检索？

**A**: 
在 `FieldDefinition` 中指定不同的 analyzer:

```java
FieldDefinition.builder()
    .fieldName("content_en")
    .indexType(IndexType.TEXT)
    .analyzerName("rag_analyzer")  // 英文
    .build();

FieldDefinition.builder()
    .fieldName("content_zh")
    .indexType(IndexType.TEXT)
    .analyzerName("rag_cjk_analyzer")  // 中文
    .build();
```

### Q5: 如何调试 Mapping 是否正确？

**A**: 使用 ES API 查看实际 Mapping:

```bash
GET /rag_kb_tenant001_kb123456/_mapping
```

或在代码中:

```java
esClient.indices().getMapping(g -> g.index(indexName));
```

---

## 11. 总结

本文档详细介绍了 RAG 平台的 Elasticsearch Mapping 设计：

1. **Index Template**: 统一配置所有知识库索引
2. **Dynamic Templates**: 自动处理向量和文本字段
3. **系统字段**: 统一的元数据管理
4. **动态生成**: 根据业务元数据自动生成 Mapping
5. **性能优化**: HNSW 参数、分片策略、刷新策略

通过元数据驱动的设计，系统能够灵活适配不同业务场景，同时保持索引结构的规范性和性能优化。

---

**相关文档**:
- [核心设计文档](./核心设计文档.md)
- [Index Template 配置](../elasticsearch/index-template.json)
- [ElasticsearchIndexManager 源码](../src/main/java/com/enterprise/rag/infrastructure/elasticsearch/ElasticsearchIndexManager.java)
