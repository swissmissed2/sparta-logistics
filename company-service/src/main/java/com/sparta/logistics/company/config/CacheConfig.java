package com.sparta.logistics.company.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Hub 캐시 설정
 * - Hub 데이터는 총 17개로 구성된 고정성 높은 데이터이며,
 *   변경 빈도 또한 매우 낮은 특성을 가집니다.
 *
 * - 팀 논의 결과, 현재 구조에서는 TTL 10분 캐싱 전략으로도
 *   안정적인 운영이 가능하다고 판단되었습니다.
 *
 * - 따라서 별도의 캐시 무효화(@CacheEvict) 전략은
 *   현재 단계에서는 적용하지 않으며,
 *   추후 요구사항 변경 시 재검토할 예정입니다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        // Redis 직렬화 방식 설정
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 캐시별 TTL 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("hubs", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("hubs-batch", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
