package com.careerai.builder.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Redis Cache Configuration
 * Enables Spring Caching with Redis backend
 *
 * TTLs defined in CacheManager via application.yml:
 * - userLatestCv: 5 minutes (CV changes frequently)
 * - cvReview: 30 minutes (review is stable)
 * - cvSkills: 15 minutes (used for roadmap generation)
 * - userRoadmaps: 10 minutes (roadmaps can be regenerated)
 * - roadmapSteps: 15 minutes (steps within a roadmap)
 * - skillMapping: 60 minutes (skill normalization cache)
 *
 * Spring Boot auto-configures:
 * - RedisConnectionFactory (from spring-boot-starter-data-redis)
 * - RedisTemplate (configured via application.yml)
 * - RedisCacheManager (configured via spring.cache.redis properties)
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@Slf4j
public class CacheConfig {

    public CacheConfig() {
        log.info("✓ CacheConfig initialized - @EnableCaching activated");
        log.info("✓ Redis cache backend configured via application.yml properties");
        log.info("✓ Multiple cache regions with individualized TTLs ready");
    }
}
