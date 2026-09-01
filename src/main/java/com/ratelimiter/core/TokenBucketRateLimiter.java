package com.ratelimiter.core;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket rate limiter implementation with thread-safe concurrent access.
 * Uses per-client locks to minimize contention while maintaining accuracy.
 *
 * The token bucket algorithm allows for bursty traffic up to the bucket capacity
 * while enforcing a sustained rate limit over time.
 */
public final class TokenBucketRateLimiter implements RateLimiter {
    private static final class ClientBucket {
        private double tokens;
        private long lastRefillTime;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        ClientBucket(int capacity) {
            this.tokens = capacity;
            this.lastRefillTime = System.nanoTime();
        }
    }

    private final TokenBucketConfig config;
    private final ConcurrentHashMap<String, ClientBucket> buckets;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong rejectedRequests = new AtomicLong(0);

    public TokenBucketRateLimiter(TokenBucketConfig config) {
        this.config = config;
        this.buckets = new ConcurrentHashMap<>();
    }

    @Override
    public RateLimitResult tryAcquire(String clientId) {
        return tryAcquire(clientId, 1);
    }

    @Override
    public RateLimitResult tryAcquire(String clientId, int tokens) {
        if (tokens <= 0) {
            throw new IllegalArgumentException("tokens must be positive");
        }
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId must not be null or empty");
        }

        totalRequests.incrementAndGet();
        ClientBucket bucket = buckets.computeIfAbsent(clientId, _ -> new ClientBucket(config.getCapacity()));

        bucket.lock.writeLock().lock();
        try {
            refillBucket(bucket);

            if (bucket.tokens >= tokens) {
                bucket.tokens -= tokens;
                allowedRequests.incrementAndGet();
                return new Allowed(Instant.now());
            } else {
                rejectedRequests.incrementAndGet();
                long retryAfterMs = calculateRetryAfter(bucket, tokens);
                return new Rejected(Instant.now(), retryAfterMs);
            }
        } finally {
            bucket.lock.writeLock().unlock();
        }
    }

    @Override
    public void reset(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId must not be null or empty");
        }

        ClientBucket bucket = buckets.get(clientId);
        if (bucket != null) {
            bucket.lock.writeLock().lock();
            try {
                bucket.tokens = config.getCapacity();
                bucket.lastRefillTime = System.nanoTime();
            } finally {
                bucket.lock.writeLock().unlock();
            }
        }
    }

    @Override
    public RateLimiterStats getStats() {
        return new RateLimiterStats(
                totalRequests.get(),
                allowedRequests.get(),
                rejectedRequests.get(),
                buckets.size()
        );
    }

    /**
     * Refills the bucket based on elapsed time since last refill.
     * Called under write lock.
     */
    private void refillBucket(ClientBucket bucket) {
        long now = System.nanoTime();
        long elapsedNanos = now - bucket.lastRefillTime;
        long elapsedMs = elapsedNanos / 1_000_000;

        double tokensToAdd = (elapsedMs / (double) config.getRefillIntervalMs()) * 1;
        bucket.tokens = Math.min(config.getCapacity(), bucket.tokens + tokensToAdd);
        bucket.lastRefillTime = now;
    }

    /**
     * Calculates approximate milliseconds until retry is possible.
     * Called under write lock.
     */
    private long calculateRetryAfter(ClientBucket bucket, int requestedTokens) {
        double tokensNeeded = requestedTokens - bucket.tokens;
        double refillsNeeded = Math.ceil(tokensNeeded);
        return (long) (refillsNeeded * config.getRefillIntervalMs());
    }
}
