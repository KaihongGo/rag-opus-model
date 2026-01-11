package com.enterprise.rag.domain.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.enterprise.rag.domain.model.knowledgebase.FieldDefinition;
import com.enterprise.rag.domain.model.knowledgebase.KnowledgeBase;
import com.enterprise.rag.domain.model.search.RetrievalMethod;
import com.enterprise.rag.domain.model.search.RetrievalMethodType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文本检索策略实现
 * 
 * <p>使用 Elasticsearch 的 match/multi_match 查询实现全文搜索</p>
 */
@Slf4j
@Component
public class TextSearchStrategy implements SearchStrategy {
    
    @Override
    public RetrievalMethodType getSupportedType() {
        return RetrievalMethodType.TEXT;
    }
    
    @Override
    public SearchRequest.Builder buildSearchRequest(
            KnowledgeBase knowledgeBase,
            String queryText,
            RetrievalMethod retrievalMethod,
            float[] queryVector,
            int topK) {
        
        if (queryText == null || queryText.isBlank()) {
            throw new IllegalArgumentException("Query text is required for text search");
        }
        
        // 确定搜索字段
        List<String> searchFields = determineSearchFields(knowledgeBase, retrievalMethod);
        
        // 构建查询
        Query query = buildTextQuery(queryText, searchFields, retrievalMethod);
        
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(knowledgeBase.getIndexName())
                .query(query)
                .size(topK);
        
        // 添加高亮
        builder.highlight(h -> h
                .fields("*", f -> f
                        .preTags("<em>")
                        .postTags("</em>")
                        .fragmentSize(150)
                        .numberOfFragments(3)
                )
        );
        
        log.debug("Built text search request for index {} with fields {}, topK={}",
                knowledgeBase.getIndexName(), searchFields, topK);
        
        return builder;
    }
    
    /**
     * 构建文本查询
     */
    private Query buildTextQuery(String queryText, List<String> fields, RetrievalMethod retrievalMethod) {
        RetrievalMethod.TextSearchConfig textConfig = retrievalMethod.getTextConfig();
        String matchType = textConfig != null ? textConfig.getMatchType() : "best_fields";
        String fuzziness = textConfig != null ? textConfig.getFuzziness() : "AUTO";
        Map<String, Float> fieldBoosts = textConfig != null ? textConfig.getFieldBoosts() : null;
        
        // 构建带 boost 的字段列表
        List<String> boostedFields = buildBoostedFields(fields, fieldBoosts);
        
        if (fields.size() == 1) {
            // 单字段使用 match 查询
            return Query.of(q -> q
                    .match(m -> m
                            .field(fields.get(0))
                            .query(queryText)
                            .fuzziness(fuzziness)
                    )
            );
        } else {
            // 多字段使用 multi_match 查询
            return Query.of(q -> q
                    .multiMatch(mm -> mm
                            .query(queryText)
                            .fields(boostedFields)
                            .type(parseTextQueryType(matchType))
                            .fuzziness(fuzziness)
                    )
            );
        }
    }
    
    /**
     * 确定搜索字段
     */
    private List<String> determineSearchFields(KnowledgeBase knowledgeBase, RetrievalMethod retrievalMethod) {
        // 如果指定了目标字段
        if (retrievalMethod.getTargetFields() != null && !retrievalMethod.getTargetFields().isEmpty()) {
            return retrievalMethod.getTargetFields();
        }
        
        // 默认使用所有文本字段
        List<FieldDefinition> textFields = knowledgeBase.getTextFields();
        if (textFields.isEmpty()) {
            throw new IllegalStateException("No text fields defined in knowledge base");
        }
        
        return textFields.stream()
                .map(FieldDefinition::getFieldName)
                .collect(Collectors.toList());
    }
    
    /**
     * 构建带 boost 的字段列表
     */
    private List<String> buildBoostedFields(List<String> fields, Map<String, Float> fieldBoosts) {
        if (fieldBoosts == null || fieldBoosts.isEmpty()) {
            return fields;
        }
        
        List<String> boostedFields = new ArrayList<>();
        for (String field : fields) {
            Float boost = fieldBoosts.get(field);
            if (boost != null) {
                boostedFields.add(field + "^" + boost);
            } else {
                boostedFields.add(field);
            }
        }
        return boostedFields;
    }
    
    /**
     * 解析文本查询类型
     */
    private TextQueryType parseTextQueryType(String matchType) {
        return switch (matchType.toLowerCase()) {
            case "best_fields" -> TextQueryType.BestFields;
            case "most_fields" -> TextQueryType.MostFields;
            case "cross_fields" -> TextQueryType.CrossFields;
            case "phrase" -> TextQueryType.Phrase;
            case "phrase_prefix" -> TextQueryType.PhrasePrefix;
            case "bool_prefix" -> TextQueryType.BoolPrefix;
            default -> TextQueryType.BestFields;
        };
    }
}
