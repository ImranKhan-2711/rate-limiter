package com.ratelimiter.core;

/**
 * Configuration for token bucket rate limiter.
 */
public final class TokenBucketConfig {
    private final int capacity;
    private final int tokensPerSecond;
    private final long refillIntervalMs;

    /**
     * Creates a token bucket configuration.
     *
     * @param capacity maximum number of tokens in the bucket
     * @param tokensPerSecond rate at which tokens are refilled
     */
    public TokenBucketConfig(int capacity, int tokensPerSecond) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (tokensPerSecond <= 0) {
            throw new IllegalArgumentException("tokensPerSecond must be positive");
        }
        this.capacity = capacity;
        this.tokensPerSecond = tokensPerSecond;
        this.refillIntervalMs = 1000L / tokensPerSecond;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTokensPerSecond() {
        return tokensPerSecond;
    }

    public long getRefillIntervalMs() {
        return refillIntervalMs;
    }
}
