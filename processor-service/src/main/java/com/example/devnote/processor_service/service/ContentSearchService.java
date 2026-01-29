package com.example.devnote.processor_service.service;

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.example.devnote.processor_service.es.EsContent;
import com.example.devnote.processor_service.es.EsSearchLog;
import com.example.devnote.processor_service.es.EsSearchLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentSearchService {
    private final EsSearchLogRepository searchLogRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate redisTemplate;
    private static final Pattern SAFE_SEARCH_TERM =
            Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s._\\-+#]{1,50}$");


    /**
     * 키워드로 콘텐츠를 검색하고, IP기반으로 10분에 한 번씩 검색어 로그 남기기
     */
    public Page<EsContent> search(String keyword, String source, String category, String sortOption, Pageable pageable, HttpServletRequest request) {
        // 검색어 유효성 검사
        if (!isValidSearchTerm(keyword)) {
            return Page.empty(pageable);
        }

        // 검색어 로깅 시도
        logSearchQuery(keyword, source, request);

        Sort sort = buildSort(sortOption);
        Pageable finalPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // Bool 쿼리 구조를 must와 filter로 분리
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> {
                            // 1. 점수 계산에 영향을 주는 검색어 쿼리는 'must' 절에 삽입
                            b.must(m -> m
                                    .multiMatch(mm -> mm
                                            .query(keyword)
                                            .fields("title", "description", "channelTitle")
                                    )
                            );

                            // 2. 점수 계산 없이 필터링만 하는 조건들은 'filter' 절에 삽입
                            b.filter(f -> f
                                    .term(t -> t
                                            .field("source")
                                            .value(source.toUpperCase())
                                    )
                            );

                            // 3. category 파라미터가 있을 경우에만 category 필터 조건을 추가
                            if (category != null && !category.isBlank()) {
                                b.filter(f -> f
                                        .term(t -> t
                                                .field("category")
                                                .value(category)
                                        )
                                );
                            }

                            // 4. ACTIVE 상태인 콘텐츠만 검색 (HIDDEN 제외)
                            b.filter(f -> f
                                    .bool(fb -> fb
                                            .should(s -> s
                                                    .term(t -> t
                                                            .field("status")
                                                            .value("ACTIVE")
                                                    )
                                            )
                                            // status 필드가 없는 기존 문서도 포함 (하위 호환)
                                            .should(s -> s
                                                    .bool(nb -> nb
                                                            .mustNot(mn -> mn
                                                                    .exists(ex -> ex
                                                                            .field("status")
                                                                    )
                                                            )
                                                    )
                                            )
                                            .minimumShouldMatch("1")
                                    )
                            );

                            return b;
                        })
                )
                .withPageable(finalPageable)
                .build();

        SearchHits<EsContent> searchHits = elasticsearchOperations.search(query, EsContent.class);

        return org.springframework.data.support.PageableExecutionUtils.getPage(
                searchHits.getSearchHits().stream().map(org.springframework.data.elasticsearch.core.SearchHit::getContent).toList(),
                finalPageable,
                searchHits::getTotalHits
        );
    }

    // 유효성 검사 로직
    private boolean isValidSearchTerm(String term) {
        if (term == null) return false;

        String trimmed = term.trim();
        if (trimmed.isEmpty()) return false;

        // 정규식 매칭 여부 확인
        return SAFE_SEARCH_TERM.matcher(trimmed).matches();
    }


    /**
     * 파라미터 값에 따라 Sort 객체를 생성하는 메서드
     */
    private Sort buildSort(String sortOption) {
        return switch (sortOption.toLowerCase()) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "publishedAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt");

            // relevance(정확도순)일 경우, 정렬 없음을 명시하면 Elasticsearch 기본값인 연관도 점수(_score)로 정렬
            default -> Sort.unsorted();
        };
    }

    /**
     * 검색어 로깅 (IP 기반 10분 제한 추가)
     */
    private void logSearchQuery(String query, String source, HttpServletRequest request) {
        String trimmedQuery = query.trim();

        // 1. 유효성 검사 (빈 문자열 및 숫자만 있는 경우 제외)
        if (trimmedQuery.isEmpty() || isNumeric(trimmedQuery)) {
            return;
        }

        // 2. IP 추출 및 중복 체크 (10분 제한)
        String ipAddress = Optional.ofNullable(request.getHeader("X-Forwarded-For")).orElse(request.getRemoteAddr());
        String redisKey = "search_log_limit:ip:" + ipAddress + ":" + trimmedQuery;
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofMinutes(10));


        // 2. 유효성 검사
        if (trimmedQuery.isEmpty() || trimmedQuery.length() < 2) {
            return;
        }

        if (Boolean.TRUE.equals(isFirstTime)) {
            // 3. 한국 시간 생성
            String kstNow = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 4. 키워드 추출
            List<String> keywords = extractKeywordsWithNori(trimmedQuery);

            if (!keywords.isEmpty()) {
                // 명사가 추출된 경우: 각 명사를 저장
                for (String word : keywords) {
                    if (word.length() >= 2) {
                        saveLogToEs(word, source, ipAddress, kstNow);
                    }
                }
            } else {
                // 명사 추출 실패 시: 2~10자 사이인 경우 원본 저장
                if (trimmedQuery.length() >= 2 && trimmedQuery.length() <= 10) {
                    saveLogToEs(trimmedQuery, source, ipAddress, kstNow);
                }
            }
        }
    }

    /**
     * Nori를 사용한 형태소 분석
     */
    private List<String> extractKeywordsWithNori(String text) {
        List<String> keywords = new ArrayList<>();

        try (KoreanAnalyzer analyzer = new KoreanAnalyzer(
                null,
                KoreanTokenizer.DecompoundMode.MIXED,
                Set.of(),
                false
        )) {
            TokenStream tokenStream = analyzer.tokenStream("query", new StringReader(text));
            CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
            PartOfSpeechAttribute posAtt = tokenStream.addAttribute(PartOfSpeechAttribute.class);

            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                String term = termAtt.toString();
                POS.Tag pos = posAtt.getLeftPOS(); // 품사 정보 (POS.Tag enum 반환)

                // NNG(일반명사), NNP(고유명사), SL(외국어)만 추출
                if (pos == POS.Tag.NNG || pos == POS.Tag.NNP || pos == POS.Tag.SL) {
                    keywords.add(term);
                }
            }
            tokenStream.end();
        } catch (Exception e) {
            log.error("Nori analysis failed: {}", text, e);
        }
        return keywords;
    }

    /**
     * Elasticsearch 저장 로직
     */
    private void saveLogToEs(String word, String source, String ip, String kstTime) {
        try {
            EsSearchLog logRecord = EsSearchLog.builder()
                    .query(word)
                    .source(source)
                    .ip(ip)
                    .searchTime(kstTime)
                    .timestamp(Instant.now())
                    .build();

            searchLogRepository.save(logRecord);
            log.debug("Search log saved: {}", word);
        } catch (Exception e) {
            log.error("Failed to save search log to Elasticsearch: {}", word, e);
        }
    }

    /**
     * 숫자로만 된 검색어인지 체크
     */
    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    /**
     * 'search_as_you_type' 필드를 사용하여 검색어 제안 목록을 반환
     * @param keyword 사용자가 입력 중인 키워드
     * @param source 필터링할 소스
     * @return 추천 검색어 목록 (콘텐츠 제목 또는 채널명)
     */
    public List<String> getSuggestions(String keyword, String source) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        // AND 조건으로 묶기 위해 bool 쿼리 사용
                        .bool(b -> b
                                // 1. 키워드 매칭 조건
                                .must(m -> m
                                        .multiMatch(mm -> mm
                                                .query(keyword)
                                                .type(TextQueryType.BoolPrefix)
                                                .fields("title.suggest", "channelTitle.suggest")
                                        )
                                )
                                // 2. 소스(YOUTUBE/NEWS) 필터링 조건
                                .filter(f -> f
                                        .term(t -> t
                                                .field("source")
                                                .value(source.toUpperCase())
                                        )
                                )
                        )
                )
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<EsContent> searchHits = elasticsearchOperations.search(query, EsContent.class);

        // 검색된 문서의 원본 제목과 채널명을 중복 없이 리스트로 만들어 반환
        return searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getTitle())
                .distinct()
                .collect(Collectors.toList());
    }
}
