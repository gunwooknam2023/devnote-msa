package com.example.devnote.processor_service.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsContentRepository extends ElasticsearchRepository<EsContent, Long> {
}
