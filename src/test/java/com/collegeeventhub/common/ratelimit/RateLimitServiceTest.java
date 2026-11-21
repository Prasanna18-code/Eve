package com.collegeeventhub.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.getLogin().setCapacity(2);
        properties.getLogin().setWindowSeconds(60);
        properties.getRegistration().setCapacity(1);
        properties.getRegistration().setWindowSeconds(3600);
        rateLimitService = new RateLimitService(properties);
    }

    @Test
    void allowsRequestsUpToLoginCapacityThenRejects() {
        assertTrue(rateLimitService.tryConsumeLogin("127.0.0.1").allowed());
        assertTrue(rateLimitService.tryConsumeLogin("127.0.0.1").allowed());

        RateLimitService.Decision denied =
                rateLimitService.tryConsumeLogin("127.0.0.1");

        assertFalse(denied.allowed());
        assertTrue(denied.retryAfterSeconds() >= 1);
    }

    @Test
    void keepsClientBucketsIndependent() {
        assertTrue(rateLimitService.tryConsumeLogin("127.0.0.1").allowed());
        assertTrue(rateLimitService.tryConsumeLogin("127.0.0.1").allowed());

        assertTrue(rateLimitService.tryConsumeLogin("127.0.0.2").allowed());
    }

    @Test
    void appliesRegistrationCapacityPerClient() {
        assertTrue(rateLimitService.tryConsumeRegistration("127.0.0.1").allowed());
        assertFalse(rateLimitService.tryConsumeRegistration("127.0.0.1").allowed());
        assertTrue(rateLimitService.tryConsumeRegistration("127.0.0.2").allowed());
    }
}
