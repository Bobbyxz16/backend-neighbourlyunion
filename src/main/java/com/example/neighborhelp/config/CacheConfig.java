package com.example.neighborhelp.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Cache Configuration for NeighborHelp Application
 *
 * Enables caching for performance optimization, particularly for:
 * - User statistics (expensive aggregate queries)
 * - Category listings (rarely change)
 * - Resource searches (popular queries)
 *
 * Cache Strategy:
 * - Development: Simple in-memory cache (ConcurrentMapCacheManager)
 * - Production: Redis cache (configured separately)
 *
 * @author NeighborHelp Team
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Simple in-memory cache for development and testing
     *
     * Uses Java's ConcurrentHashMap for thread-safe caching.
     * Not suitable for production with multiple server instances.
     *
     * Cache names configured:
     * - userStatistics: Full user statistics (5 min TTL recommended)
     * - userBasicStatistics: Lightweight statistics (3 min TTL recommended)
     * - categories: Category listings (1 hour TTL recommended)
     * - resourceSearch: Search results (10 min TTL recommended)
     *
     * @return CacheManager for development profile
     */
    @Bean
    @Profile({"dev", "test", "default"})
    public CacheManager cacheManagerDev() {
        return new ConcurrentMapCacheManager(
                "userStatistics",
                "userBasicStatistics",
                "categories",
                "resourceSearch", "cities",
                "postalCodes", "categories"
        );
    }

    /**
     * Redis cache configuration for production
     *
     * Uncomment and configure when Redis is available.
     * Provides:
     * - Distributed caching across multiple servers
     * - TTL (Time To Live) per cache
     * - Automatic eviction
     *
     * @return CacheManager for production profile
     */
    /*
    @Bean
    @Profile("prod")
    public CacheManager cacheManagerProd(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // User statistics: 5 minutes
        cacheConfigurations.put("userStatistics",
            defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Basic statistics: 3 minutes
        cacheConfigurations.put("userBasicStatistics",
            defaultConfig.entryTtl(Duration.ofMinutes(3)));

        // Categories: 1 hour (rarely change)
        cacheConfigurations.put("categories",
            defaultConfig.entryTtl(Duration.ofHours(1)));

        // Search results: 10 minutes
        cacheConfigurations.put("resourceSearch",
            defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
    */
}