package com.ratelimiter.core;

/**
 * Core rate limiter interface for concurrent request handling.
 * Thread-safe implementations must handle concurrent transactions atomically.
 */
public interface RateLimiter {
    /**
     * Attempts to acquire a permit for the given client/key.
     * This method is thread-safe and can be called concurrently.
     *
     * @param clientId the unique identifier for the client
     * @return RateLimitResult indicating if the request is allowed or rejected
     */
    RateLimitResult tryAcquire(String clientId);

    /**
     * Attempts to acquire multiple permits at once.
     * This method is thread-safe and can be called concurrently.
     *
     * @param clientId the unique identifier for the client
     * @param tokens number of tokens to acquire
     * @return RateLimitResult indicating if the request is allowed or rejected
     */
    RateLimitResult tryAcquire(String clientId, int tokens);

    /**
     * Resets the rate limit state for a specific client.
     *
     * @param clientId the unique identifier for the client
     */
    void reset(String clientId);

    /**
     * Returns statistics about the rate limiter.
     *
     * @return RateLimiterStats with current metrics
     */
    RateLimiterStats getStats();
}
