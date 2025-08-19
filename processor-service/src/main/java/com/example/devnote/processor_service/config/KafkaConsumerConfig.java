package com.example.devnote.processor_service.config;

import com.example.devnote.processor_service.dto.ContentMessageDto;
import com.example.devnote.processor_service.dto.ContentStatsUpdateDto;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer 설정
 */
@Configuration
@RequiredArgsConstructor
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {
    private final KafkaProperties props;

    /**
     * ConsumerFactory 빈 생성
     */
    @Bean
    public ConsumerFactory<String, ContentMessageDto> consumerFactory() {
        Map<String, Object> cfg = new HashMap<>(props.buildConsumerProperties());
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 신뢰할 패키지 설정
        JsonDeserializer<ContentMessageDto> deserializer =
                new JsonDeserializer<>(ContentMessageDto.class, false);
        deserializer.addTrustedPackages("com.example.devnote.processorservice.dto");

        return new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), deserializer);
    }

    /**
     * ConcurrentKafkaListenerContainerFactory 빈 생성
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ContentMessageDto>
    kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ContentMessageDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        log.info("KafkaListenerContainerFactory configured");
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ContentStatsUpdateDto> statsUpdateConsumerFactory() {
        Map<String, Object> cfg = new HashMap<>(props.buildConsumerProperties());
        // 통계 업데이트용 Consumer Group ID를 별도로 지정
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "processor-service-group-stats");

        JsonDeserializer<ContentStatsUpdateDto> deserializer =
                new JsonDeserializer<>(ContentStatsUpdateDto.class, false);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ContentStatsUpdateDto>
    statsUpdateContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ContentStatsUpdateDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(statsUpdateConsumerFactory());
        return factory;
    }
}
