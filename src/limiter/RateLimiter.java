package limiter;

import model.Request;
import metrics.MetricsCollector;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    private final double refillRate;                  // tokens per second
               // original capacity
    private volatile int currentCapacity;             // adaptive capacity
    private final ConcurrentHashMap<String, TokenBucket> userBuckets;
    private final MetricsCollector metrics;

    public RateLimiter(int bucketCapacity, double refillRate, MetricsCollector metrics) {
        this.refillRate      = refillRate;
        this.currentCapacity = bucketCapacity;
        this.userBuckets     = new ConcurrentHashMap<>();
        this.metrics         = metrics;
    }

    public boolean allowRequest(Request request) {
        String userId = request.getUserId();

        TokenBucket bucket = userBuckets.computeIfAbsent(
                userId,
                id -> new TokenBucket(currentCapacity, refillRate)
        );

        boolean allowed = bucket.tryConsume();

        if (allowed) {
            metrics.recordAllowed();
        } else {
            metrics.recordRejected();
        }

        return allowed;
    }

    public int getCurrentLimit() {
        return currentCapacity;
    }

    public void setLimit(int newLimit) {
        this.currentCapacity = newLimit;
        System.out.println("[RateLimiter] Limit updated to: " + newLimit + " tokens");
    }
}