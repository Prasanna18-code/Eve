package com.collegeeventhub.config;

import com.collegeeventhub.common.cache.CacheProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheConfigTest {
    @Test
    void createsNamedCaffeineCachesWithConfiguredMaximumSize() {
        CacheProperties properties = new CacheProperties();
        properties.setMaximumSize(100);
        properties.setTtlMinutes(1);

        CacheManager manager = new CacheConfig(properties).cacheManager();

        assertNotNull(manager.getCache("events"));
        assertNotNull(manager.getCache("resources"));
        assertNotNull(manager.getCache("clubs"));
        assertNotNull(manager.getCache("organizations"));
        assertNotNull(manager.getCache("studentDirectory"));
        org.junit.jupiter.api.Assertions.assertInstanceOf(
                CaffeineCacheManager.class,
                manager
        );
    }
}
