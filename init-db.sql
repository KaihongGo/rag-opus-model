-- RAG Platform Database Schema

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATING',
    field_definitions JSONB NOT NULL DEFAULT '[]',
    index_name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_knowledge_base_tenant_id ON knowledge_base(tenant_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_status ON knowledge_base(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_index_name ON knowledge_base(index_name);

-- 文档元数据表 (可选，用于追踪)
CREATE TABLE IF NOT EXISTS document_metadata (
    id VARCHAR(36) PRIMARY KEY,
    knowledge_base_id VARCHAR(36) NOT NULL REFERENCES knowledge_base(id),
    source_document_id VARCHAR(255),
    chunk_index INTEGER,
    status VARCHAR(32) NOT NULL DEFAULT 'INDEXED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_metadata_kb_id ON document_metadata(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_document_metadata_source ON document_metadata(source_document_id);

-- 摄取任务表 (可选，用于异步任务追踪)
CREATE TABLE IF NOT EXISTS ingestion_task (
    id VARCHAR(36) PRIMARY KEY,
    knowledge_base_id VARCHAR(36) NOT NULL REFERENCES knowledge_base(id),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_documents INTEGER NOT NULL DEFAULT 0,
    processed_documents INTEGER NOT NULL DEFAULT 0,
    failed_documents INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ingestion_task_kb_id ON ingestion_task(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_task_status ON ingestion_task(status);

-- 示例知识库数据
INSERT INTO knowledge_base (id, name, description, tenant_id, status, field_definitions, index_name, created_at, version)
VALUES (
    'demo-kb-001',
    'Demo Knowledge Base',
    'A demonstration knowledge base for RAG retrieval',
    'demo-tenant',
    'ACTIVE',
    '[
        {
            "fieldName": "content",
            "indexType": "VECTOR",
            "isFilter": false,
            "required": true,
            "description": "Main content field for semantic search",
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
            "required": true,
            "description": "Document title for text search"
        },
        {
            "fieldName": "category",
            "indexType": "KEYWORD",
            "isFilter": true,
            "required": false,
            "description": "Document category for filtering"
        },
        {
            "fieldName": "summary",
            "indexType": "TEXT",
            "isFilter": false,
            "required": false,
            "description": "Document summary"
        }
    ]'::jsonb,
    'rag_kb_demo_tenant_demokb001',
    CURRENT_TIMESTAMP,
    0
) ON CONFLICT (id) DO NOTHING;
