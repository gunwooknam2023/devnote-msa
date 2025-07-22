package com.example.devnote.processor_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정
 */
@Configuration
@Slf4j
public class RedisConfig {
    /**
     * RedisTemplate 빈 생성
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);

        // Key: String
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setHashKeySerializer(new StringRedisSerializer());

        // Value: JSON
        GenericJackson2JsonRedisSerializer ser = new GenericJackson2JsonRedisSerializer();
        tpl.setValueSerializer(ser);
        tpl.setHashValueSerializer(ser);

        tpl.afterPropertiesSet();
        log.info("RedisTemplate configured");
        return tpl;
    }
}
