package com.example.devnote.news_youtube_service.config;


import com.example.devnote.news_youtube_service.dto.ChannelStatsUpdateDto;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer 설정
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;

    /**
     * 채널 통계 업데이트(ChannelStatsUpdateDto) 메시지를 처리하기 위한 ConsumerFactory를 생성
     */
    @Bean
    public ConsumerFactory<String, ChannelStatsUpdateDto> statsUpdateConsumerFactory() {
        // application.yml의 Kafka 설정을 기반으로 기본 소비자 속성을 가져옵니다.
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        // 통계 업데이트용 Consumer Group ID를 별도로 지정합니다.
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "news-youtube-service-group-stats");

        // JSON 형식의 메시지를 ChannelStatsUpdateDto 객체로 역직렬화(deserialization)하기 위한 설정입니다.
        JsonDeserializer<ChannelStatsUpdateDto> deserializer =
                new JsonDeserializer<>(ChannelStatsUpdateDto.class, false);
        // 모든 패키지의 클래스를 신뢰하도록 설정하여 역직렬화 오류를 방지합니다.
        deserializer.addTrustedPackages("*");

        // 키는 StringDeserializer, 값은 위에서 설정한 JsonDeserializer를 사용하여 Consumer를 생성합니다.
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /**
     * @KafkaListener가 사용할 컨테이너 팩토리를 생성
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChannelStatsUpdateDto> statsUpdateContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChannelStatsUpdateDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(statsUpdateConsumerFactory());
        return factory;
    }
}