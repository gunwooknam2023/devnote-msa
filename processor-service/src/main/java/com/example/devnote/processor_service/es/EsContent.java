package com.example.devnote.processor_service.es;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

/**
 * Elasticsearch 'contents' 인덱스에 저장될 문서
 */
@Data
@Builder
@Document(indexName = "contents")
public class EsContent {
    @Id
    private Long id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean_analyzer"), // (1) 전문 검색용 (형태소 분석)
            otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) } // (2) 정확한 값 정렬/집계용
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String description;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean_analyzer"),
            otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private String channelTitle;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Keyword)
    private String channelId;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant publishedAt;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant createdAt;

    @Field(type = FieldType.Long)
    private Long viewCount;

    @Field(type = FieldType.Long)
    private Long localViewCount;

    @Field(type = FieldType.Long)
    private Long durationSeconds;

    @Field(type = FieldType.Long)
    private Long subscriberCount;

    @Field(type = FieldType.Long)
    private Long favoriteCount;

    @Field(type = FieldType.Long)
    private Long commentCount;
}
