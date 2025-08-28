package com.example.devnote.processor_service.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsSearchLogRepository extends ElasticsearchRepository<EsSearchLog, String> {
}
