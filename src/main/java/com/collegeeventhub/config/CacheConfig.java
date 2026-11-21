package com.collegeeventhub.config;

import com.collegeeventhub.common.cache.CacheProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {
    private final CacheProperties properties;

    public CacheConfig(CacheProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(
                "events",
                "resources",
                "clubs",
                "organizations",
                "studentDirectory"
        ));
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(properties.getMaximumSize())
                .expireAfterWrite(
                        properties.getTtlMinutes(),
                        TimeUnit.MINUTES
                ));
        return manager;
    }
}
