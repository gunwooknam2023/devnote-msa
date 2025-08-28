package com.example.devnote.stats_service.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Data
@Document(indexName = "search_logs")
public class EsSearchLog {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String query;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant timestamp;
}