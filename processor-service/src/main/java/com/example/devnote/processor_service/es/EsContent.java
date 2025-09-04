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
            mainField = @Field(type = FieldType.Text, analyzer = "korean_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword),
                    @InnerField(suffix = "suggest", type = FieldType.Search_As_You_Type, analyzer = "korean_analyzer")
            }
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

    @Field(type = FieldType.Keyword, index = false)
    private String link;

    @Field(type = FieldType.Keyword, index = false)
    private String channelThumbnailUrl;

    @Field(type = FieldType.Keyword)
    private String videoForm;

    @Field(type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

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
