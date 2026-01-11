# RAG Retrieval Platform - Copilot Instructions

## Architecture Overview

This is a **DDD-based enterprise RAG platform** with strict four-layer architecture:

```
interfaces/ → application/ → domain/ → infrastructure/
```

**Key design decisions:**
- **Metadata-driven**: `KnowledgeBase` aggregate root dynamically defines field structures via `FieldDefinition` - no hardcoded schemas
- **Strategy Pattern**: Both search (`SearchStrategy`) and fusion (`FusionStrategy`) use strategy pattern with factory injection
- **Multi-tenant**: All entities include `tenantId`; ES index names follow pattern `rag_kb_{tenantId}_{id}`

## Layer Responsibilities & Patterns

### Domain Layer (`domain/`)
- **Aggregate root**: `KnowledgeBase` owns `FieldDefinition` collection
- **Value objects**: `SearchRequest`, `SearchResult`, `RetrievalMethod`, `EmbeddingConfig`
- **Repository interfaces only** - no implementations
- Add business logic to domain model methods (e.g., `KnowledgeBase.canSearch()`, `FieldDefinition.validate()`)

### Application Layer (`application/service/`)
- Coordinates cross-aggregate operations
- `SearchService` orchestrates KB lookup → fusion processing → result return
- `IngestionService` handles async document ingestion from Kafka

### Infrastructure Layer (`infrastructure/`)
- `ElasticsearchIndexManager`: Dynamically creates ES indices from `KnowledgeBase` metadata
- `KnowledgeBaseRepositoryImpl`: Implements domain repository, uses `KnowledgeBaseConverter` for PO↔Domain mapping
- Kafka consumer uses manual ack (`Acknowledgment.acknowledge()`)

### Interfaces Layer (`interfaces/rest/`)
- DTOs are separate from domain models - use `DtoAssembler` for conversion
- Controllers delegate to application services only

## Adding New Search/Fusion Strategies

1. **Search Strategy**: Implement `SearchStrategy`, return type from `getSupportedType()` - auto-registered via Spring
   ```java
   @Component
   public class HybridSearchStrategy implements SearchStrategy {
       @Override public RetrievalMethodType getSupportedType() { return RetrievalMethodType.HYBRID; }
   }
   ```

2. **Fusion Strategy**: Implement `FusionStrategy`, return type from `getType()` - auto-registered
   ```java
   @Component  
   public class CustomFusionStrategy implements FusionStrategy {
       @Override public FusionStrategyType getType() { return FusionStrategyType.CUSTOM; }
   }
   ```

## Elasticsearch Patterns

- Uses **ES 8.x Java API Client** (`co.elastic.clients.elasticsearch`)
- Vector fields use `dense_vector` with configurable similarity (cosine/dot_product/l2_norm)
- System fields are prefixed with `_` (e.g., `_doc_id`, `_knowledge_base_id`)
- Vector storage field naming: `{fieldName}_vector` (see `FieldDefinition.getVectorFieldName()`)

## Development Commands

```bash
# Start dependencies (Postgres, ES, Kafka, Zookeeper)
docker-compose up -d

# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Application runs on port 8080
# Kibana available at localhost:5601
```

## API Patterns

- **Search**: `POST /api/v1/search` with `SearchRequestDTO` containing `retrievalMethods[]` and `fusionStrategy`
- **Quick endpoints**: `GET /api/v1/search/vector?query=...` and `GET /api/v1/search/text?query=...`
- **Knowledge Base CRUD**: `/api/v1/knowledge-bases`
- Ingestion via Kafka topic `rag.ingestion.raw` (not REST)

## Key Configuration (`application.yml`)

- `embedding.service.base-url`: External embedding service endpoint
- `rag.index.prefix`: ES index prefix (default: `rag_kb_`)
- `rag.search.default-top-k`: Default result count
- `rag.ingestion.topic`: Kafka topic for document ingestion
