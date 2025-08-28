package com.example.devnote.stats_service.es;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.Instant;
import java.util.List;

public interface EsSearchLogRepository extends ElasticsearchRepository<EsSearchLog, String> {
    List<EsSearchLog> findByTimestampBetween(Instant start, Instant end, Pageable pageable);
}