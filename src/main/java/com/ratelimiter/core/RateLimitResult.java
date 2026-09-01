package com.ratelimiter.core;

import java.time.Instant;

/**
 * Represents a rate limit decision for a request.
 */
public sealed interface RateLimitResult permits Allowed, Rejected {
    /**
     * @return true if the request is allowed
     */
    boolean isAllowed();

    /**
     * @return the timestamp when this decision was made
     */
    Instant decidedAt();
}

/**
 * Request is allowed to proceed.
 */
final record Allowed(Instant decidedAt) implements RateLimitResult {
    @Override
    public boolean isAllowed() {
        return true;
    }
}

/**
 * Request is rejected due to rate limit exceeded.
 */
final record Rejected(Instant decidedAt, long retryAfterMs) implements RateLimitResult {
    @Override
    public boolean isAllowed() {
        return false;
    }

    /**
     * @return milliseconds to wait before retrying
     */
    public long retryAfterMs() {
        return retryAfterMs;
    }
}
