#!/bin/bash

# RAG Platform - Elasticsearch 初始化脚本
# 用于创建索引模板和初始化设置

ES_HOST=${ES_HOST:-"http://localhost:9200"}

echo "Waiting for Elasticsearch to be ready..."
until curl -s "$ES_HOST/_cluster/health" | grep -q '"status":"green"\|"status":"yellow"'; do
    echo "Elasticsearch is not ready yet. Retrying in 5 seconds..."
    sleep 5
done
echo "Elasticsearch is ready!"

# 创建索引模板
echo "Creating index template..."
curl -X PUT "$ES_HOST/_index_template/rag_kb_template" \
    -H "Content-Type: application/json" \
    -d @index-template.json

echo ""
echo "Index template created successfully!"

# 创建演示索引
echo "Creating demo index..."
curl -X PUT "$ES_HOST/rag_kb_demo_tenant_demokb001" \
    -H "Content-Type: application/json" \
    -d '{
        "settings": {
            "number_of_shards": 1,
            "number_of_replicas": 0
        },
        "mappings": {
            "properties": {
                "_doc_id": {"type": "keyword"},
                "_knowledge_base_id": {"type": "keyword"},
                "_created_at": {"type": "date"},
                "_updated_at": {"type": "date"},
                "_source_document_id": {"type": "keyword"},
                "_chunk_index": {"type": "integer"},
                "_metadata": {"type": "object"},
                "content": {"type": "text", "analyzer": "standard"},
                "content_vector": {
                    "type": "dense_vector",
                    "dims": 1536,
                    "index": true,
                    "similarity": "cosine"
                },
                "title": {
                    "type": "text",
                    "fields": {"keyword": {"type": "keyword"}}
                },
                "category": {"type": "keyword"},
                "summary": {"type": "text"}
            }
        }
    }'

echo ""
echo "Demo index created successfully!"

# 插入示例数据
echo "Inserting sample documents..."
curl -X POST "$ES_HOST/rag_kb_demo_tenant_demokb001/_doc/doc-001" \
    -H "Content-Type: application/json" \
    -d '{
        "_doc_id": "doc-001",
        "_knowledge_base_id": "demo-kb-001",
        "_created_at": "2024-01-01T00:00:00Z",
        "title": "Introduction to RAG Systems",
        "content": "Retrieval-Augmented Generation (RAG) is a technique that combines the power of large language models with external knowledge retrieval. RAG systems first retrieve relevant documents from a knowledge base and then use this context to generate more accurate and informed responses.",
        "category": "AI",
        "summary": "Overview of RAG technology and its applications"
    }'

curl -X POST "$ES_HOST/rag_kb_demo_tenant_demokb001/_doc/doc-002" \
    -H "Content-Type: application/json" \
    -d '{
        "_doc_id": "doc-002",
        "_knowledge_base_id": "demo-kb-001",
        "_created_at": "2024-01-02T00:00:00Z",
        "title": "Vector Search Fundamentals",
        "content": "Vector search, also known as semantic search, uses dense vector representations to find documents based on meaning rather than exact keyword matches. This enables finding semantically similar content even when different words are used.",
        "category": "Search",
        "summary": "Basics of vector-based semantic search"
    }'

curl -X POST "$ES_HOST/rag_kb_demo_tenant_demokb001/_doc/doc-003" \
    -H "Content-Type: application/json" \
    -d '{
        "_doc_id": "doc-003",
        "_knowledge_base_id": "demo-kb-001",
        "_created_at": "2024-01-03T00:00:00Z",
        "title": "Hybrid Search Strategies",
        "content": "Hybrid search combines multiple retrieval methods such as vector search and BM25 text search. Fusion algorithms like RRF (Reciprocal Rank Fusion) are used to merge results from different methods, often achieving better performance than any single method alone.",
        "category": "Search",
        "summary": "Combining vector and text search for better results"
    }'

echo ""
echo "Sample documents inserted!"
echo ""
echo "Elasticsearch initialization completed!"
