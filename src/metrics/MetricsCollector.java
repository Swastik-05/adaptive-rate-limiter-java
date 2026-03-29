package metrics;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MetricsCollector — Sliding window metrics tracker.
 *
 * Tracks:
 *   - Throughput    : total requests (allowed + rejected) in last WINDOW_SIZE_MS
 *   - Latency       : rolling average of last MAX_LATENCY_SAMPLES recorded latencies
 *   - Allowed count : lifetime count of requests that passed the rate limiter
 *   - Rejected count: lifetime count of requests that were throttled
 *
 * Thread-safe. No locks on the hot path.
 */
public class MetricsCollector {

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /** How far back (ms) we look when computing throughput. */
    private static final long WINDOW_SIZE_MS = 10_000L; // 10 seconds

    /** Maximum number of latency samples kept in memory at once. */
    private static final int MAX_LATENCY_SAMPLES = 100;

    // -------------------------------------------------------------------------
    // Internal state — throughput (sliding window)
    // -------------------------------------------------------------------------

    /**
     * Timestamps (epoch ms) of every request recorded inside the current window.
     * Includes both allowed and rejected requests.
     * Old entries are pruned lazily when throughput is queried.
     */
    private final ConcurrentLinkedQueue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();

    // -------------------------------------------------------------------------
    // Internal state — allowed / rejected counters (lifetime)
    // -------------------------------------------------------------------------

    /**
     * Total number of requests that successfully acquired a token.
     * Incremented by recordAllowed(); never decremented.
     */
    private final AtomicLong allowedRequests = new AtomicLong(0L);

    /**
     * Total number of requests that were throttled / rejected.
     * Incremented by recordRejected(); never decremented.
     */
    private final AtomicLong rejectedRequests = new AtomicLong(0L);

    // -------------------------------------------------------------------------
    // Internal state — latency (bounded rolling window)
    // -------------------------------------------------------------------------

    /**
     * Recent latency values in milliseconds.
     * Capped at MAX_LATENCY_SAMPLES — oldest entry dropped when full.
     */
    private final ConcurrentLinkedQueue<Long> latencySamples = new ConcurrentLinkedQueue<>();

    /**
     * Running total of all values currently in latencySamples.
     * Updated atomically alongside every add/remove so we avoid re-summing.
     */
    private final AtomicLong latencySum = new AtomicLong(0L);

    // -------------------------------------------------------------------------
    // Public API — write path (called from RateLimiter on every request)
    // -------------------------------------------------------------------------

    /**
     * Record that a request was allowed by the rate limiter.
     *
     * Hot path: one offer() + one incrementAndGet() — no locks, no blocking.
     */
    public void recordAllowed() {
        requestTimestamps.offer(System.currentTimeMillis()); // sliding window
        allowedRequests.incrementAndGet();                   // lifetime counter
    }

    /**
     * Record that a request was rejected (throttled) by the rate limiter.
     *
     * Hot path: one offer() + one incrementAndGet() — no locks, no blocking.
     */
    public void recordRejected() {
        requestTimestamps.offer(System.currentTimeMillis()); // still counts for throughput
        rejectedRequests.incrementAndGet();                  // lifetime counter
    }

    /**
     * Record how long (ms) a single request took end-to-end.
     * Should only be called for allowed requests that were actually processed.
     *
     * @param latencyMs elapsed time in milliseconds; negative values are ignored
     */
    public void recordLatency(long latencyMs) {
        if (latencyMs < 0) return;

        // If we are already at capacity, drop the oldest sample first.
        if (latencySamples.size() >= MAX_LATENCY_SAMPLES) {
            Long removed = latencySamples.poll();
            if (removed != null) {
                latencySum.addAndGet(-removed);
            }
        }

        latencySamples.offer(latencyMs);
        latencySum.addAndGet(latencyMs);
    }

    // -------------------------------------------------------------------------
    // Public API — read path (called by AdaptiveController)
    // -------------------------------------------------------------------------

    /**
     * Returns the total request rate (allowed + rejected) over the last
     * WINDOW_SIZE_MS milliseconds, expressed as requests-per-second.
     *
     * Prunes stale timestamps before computing — result always reflects the
     * live sliding window only.
     *
     * @return throughput in req/s; 0.0 if no requests recorded yet
     */
    public double getAverageThroughput() {
        pruneOldTimestamps();

        int count = requestTimestamps.size();
        if (count == 0) return 0.0;

        // Window is fixed at WINDOW_SIZE_MS, so rate = count / window_in_seconds
        double windowSeconds = WINDOW_SIZE_MS / 1000.0;
        return count / windowSeconds;
    }

    /**
     * Returns the mean latency (ms) across all samples currently held.
     *
     * @return average latency in ms; 0.0 if no samples recorded yet
     */
    public double getAverageLatency() {
        int count = latencySamples.size();
        if (count == 0) return 0.0;

        return (double) latencySum.get() / count;
    }

    /**
     * Returns the lifetime count of allowed requests.
     */
    public long getAllowedCount() {
        return allowedRequests.get();
    }

    /**
     * Returns the lifetime count of rejected requests.
     */
    public long getRejectedCount() {
        return rejectedRequests.get();
    }

    /**
     * Human-readable snapshot — useful for logging and debugging.
     *
     * Example output:
     *   [MetricsCollector] throughput=47.30 req/s | avgLatency=12.80 ms |
     *                      allowed=4730 | rejected=270 | latencySamples=100
     */
    public String getSummary() {
        long allowed = allowedRequests.get();
        long rejected = rejectedRequests.get();
        long total = allowed + rejected;

        return String.format(
            "[MetricsCollector]\n" +
            "Total Requests   : %d\n" +
            "Allowed Requests : %d\n" +
            "Rejected Requests: %d\n" +
            "Throughput       : %.2f req/s\n" +
            "Avg Latency      : %.2f ms\n" +
            "Acceptance Rate  : %.2f %%\n" +
            "Rejection Rate   : %.2f %%\n",
            total,
            allowed,
            rejected,
            getAverageThroughput(),
            getAverageLatency(),
            getAcceptanceRate(),
            getRejectionRate()
        );
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Removes timestamps that have fallen outside the sliding window.
     * Called lazily on every read — never on writes.
     *
     * Uses peek() before poll() to avoid discarding an entry that is still
     * inside the window — important under high concurrency.
     */
    private void pruneOldTimestamps() {
        long cutoff = System.currentTimeMillis() - WINDOW_SIZE_MS;

        while (true) {
            Long oldest = requestTimestamps.peek();
            if (oldest == null || oldest >= cutoff) break;
            requestTimestamps.poll(); // confirmed stale — discard
        }
    }
    public double getRejectionRate() {
        long total = allowedRequests.get() + rejectedRequests.get();
        if (total == 0) return 0.0;

        return (rejectedRequests.get() * 100.0) / total;
    }

    public double getAcceptanceRate() {
        long total = allowedRequests.get() + rejectedRequests.get();
        if (total == 0) return 0.0;

        return (allowedRequests.get() * 100.0) / total;
    }
}