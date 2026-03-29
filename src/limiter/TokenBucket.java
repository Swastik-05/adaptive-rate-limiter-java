package limiter;

public class TokenBucket {

    private final int capacity;
    private final double refillRate;       // tokens per second
    private double tokens;
    private long lastRefillTimestamp;      // in milliseconds

    public TokenBucket(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;            // start full
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    public synchronized boolean tryConsume() {
        refill();

        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }

        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTimestamp;

        double tokensToAdd = elapsed * (refillRate / 1000.0);

        // Always update timestamp (important for accuracy)
        lastRefillTimestamp = now;

        if (tokensToAdd > 0) {
            tokens = Math.min(capacity, tokens + tokensToAdd);
        }
    }
}