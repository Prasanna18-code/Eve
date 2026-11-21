package com.collegeeventhub.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private Policy login = new Policy();
    private Policy registration = new Policy();

    @Getter
    @Setter
    public static class Policy {
        private int capacity;
        private long windowSeconds;
    }
}
