package com.example.devnote.processor_service.es;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

/**
 * Elasticsearch 'search_logs' 인덱스에 저장될 문서
 */
@Data
@Builder
@Document(indexName = "search_logs")
public class EsSearchLog {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String query; // 검색어

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant timestamp;
}
