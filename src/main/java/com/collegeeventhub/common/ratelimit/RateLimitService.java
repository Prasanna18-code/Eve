package com.collegeeventhub.common.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RateLimitService {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public Decision tryConsumeLogin(String clientKey) {
        return tryConsume(
                "login:" + clientKey,
                properties.getLogin().getCapacity(),
                properties.getLogin().getWindowSeconds()
        );
    }

    public Decision tryConsumeRegistration(String clientKey) {
        return tryConsume(
                "registration:" + clientKey,
                properties.getRegistration().getCapacity(),
                properties.getRegistration().getWindowSeconds()
        );
    }

    private Decision tryConsume(String key, int capacity, long windowSeconds) {
        if (!properties.isEnabled()) {
            return new Decision(true, 0);
        }
        if (capacity <= 0 || windowSeconds <= 0) {
            return new Decision(true, 0);
        }

        long now = System.nanoTime();
        Bucket bucket = buckets.computeIfAbsent(
                key,
                ignored -> new Bucket(capacity, now)
        );

        synchronized (bucket) {
            bucket.refill(capacity, windowSeconds, now);
            if (bucket.tokens >= 1) {
                bucket.tokens--;
                return new Decision(true, 0);
            }

            long remainingNanos = bucket.nextRefillNanos - now;
            long retryAfter = Math.max(
                    1,
                    TimeUnit.NANOSECONDS.toSeconds(remainingNanos)
            );
            return new Decision(false, retryAfter);
        }
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillNanos;
        private long nextRefillNanos;

        private Bucket(int capacity, long now) {
            this.tokens = capacity;
            this.lastRefillNanos = now;
            this.nextRefillNanos = now;
        }

        private void refill(int capacity, long windowSeconds, long now) {
            long windowNanos = TimeUnit.SECONDS.toNanos(windowSeconds);
            long elapsed = now - lastRefillNanos;
            if (elapsed <= 0) {
                return;
            }

            double refillRate = (double) capacity / windowNanos;
            tokens = Math.min(capacity, tokens + elapsed * refillRate);
            lastRefillNanos = now;

            long nanosPerToken = windowNanos / capacity;
            if (tokens >= 1) {
                nextRefillNanos = now;
            } else {
                nextRefillNanos = now + nanosPerToken;
            }
        }
    }
}
