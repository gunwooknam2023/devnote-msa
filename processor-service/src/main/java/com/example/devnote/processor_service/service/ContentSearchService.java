package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.es.EsContent;
import com.example.devnote.processor_service.es.EsContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@Service
@RequiredArgsConstructor
public class ContentSearchService {
    private final EsContentRepository esContentRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 키워드로 콘텐츠를 검색합니다. (페이지네이션 지원)
     * @param keyword 검색어
     * @param pageable 페이지 정보
     * @return 검색된 EsContent 페이지
     */
    public Page<EsContent> search(String keyword, Pageable pageable) {
        // 'title', 'description', 'channelTitle' 필드에서 키워드를 검색하는 쿼리를 생성
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(mm -> mm
                                .query(keyword)
                                .fields("title", "description", "channelTitle")
                        )
                )
                .withPageable(pageable)
                .build();

        // 쿼리 실행
        SearchHits<EsContent> searchHits = elasticsearchOperations.search(query, EsContent.class);

        // SearchHits를 Page 객체로 변환 후 반환
        return org.springframework.data.support.PageableExecutionUtils.getPage(
                searchHits.getSearchHits().stream().map(org.springframework.data.elasticsearch.core.SearchHit::getContent).toList(),
                pageable,
                searchHits::getTotalHits
        );
    }
}
