package com.collegeeventhub.common.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cache.caffeine")
public class CacheProperties {
    private long maximumSize = 1000;
    private long ttlMinutes = 1;
}
