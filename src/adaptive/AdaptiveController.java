package adaptive;

import metrics.MetricsCollector;
import limiter.RateLimiter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AdaptiveController — Periodically reads live metrics and adjusts the
 * RateLimiter's token limit up or down based on simple thresholds.
 *
 * Decision logic (evaluated every EVALUATION_INTERVAL_SEC seconds):
 *
 *   throughput > HIGH_THROUGHPUT_THRESHOLD  →  reduce  (system overloaded)
 *   avgLatency  > HIGH_LATENCY_THRESHOLD    →  reduce  (requests slowing down)
 *   throughput < LOW_THROUGHPUT_THRESHOLD   →  increase (system underutilized)
 *   otherwise                               →  do nothing (healthy)
 *
 * Only one adjustment is made per evaluation cycle, in priority order:
 *   overload check first, underutilization check second.
 *
 * Thread-safe. Uses a single-threaded ScheduledExecutorService so the
 * evaluation loop never races with itself.
 */
public class AdaptiveController {

    // -------------------------------------------------------------------------
    // Thresholds — tune these for your system
    // -------------------------------------------------------------------------

    /** req/s above which we consider the system overloaded. */
    private static final double HIGH_THROUGHPUT_THRESHOLD = 80.0;

    /** req/s below which we consider the system underutilized. */
    private static final double LOW_THROUGHPUT_THRESHOLD = 20.0;

    /** Average latency (ms) above which we consider requests too slow. */
    private static final double HIGH_LATENCY_THRESHOLD = 200.0;

    // -------------------------------------------------------------------------
    // Adjustment step — how many tokens to add or remove per cycle
    // -------------------------------------------------------------------------

    /** Number of tokens removed from the limit on each reduce step. */
    private static final int REDUCE_STEP = 10;

    /** Number of tokens added to the limit on each increase step. */
    private static final int INCREASE_STEP = 5;

    /** Hard floor — token limit will never drop below this value. */
    private static final int MIN_TOKENS = 10;

    /** Hard ceiling — token limit will never rise above this value. */
    private static final int MAX_TOKENS = 200;

    // -------------------------------------------------------------------------
    // Evaluation cadence
    // -------------------------------------------------------------------------

    /** How often (seconds) the controller wakes up and evaluates metrics. */
    private static final int EVALUATION_INTERVAL_SEC = 5;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final MetricsCollector metrics;
    private final RateLimiter      rateLimiter;

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "adaptive-controller");
            t.setDaemon(true); // don't block JVM shutdown
            return t;
        });

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param metrics     live metrics source — only reads, never writes
     * @param rateLimiter the limiter whose token capacity will be adjusted
     */
    public AdaptiveController(MetricsCollector metrics, RateLimiter rateLimiter) {
        this.metrics     = metrics;
        this.rateLimiter = rateLimiter;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Start the periodic evaluation loop.
     * Safe to call once; calling multiple times has no effect beyond scheduling
     * duplicate tasks (avoid doing so).
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
            this::evaluate,
            EVALUATION_INTERVAL_SEC,  // initial delay — let system warm up first
            EVALUATION_INTERVAL_SEC,  // period
            TimeUnit.SECONDS
        );
        System.out.println("[AdaptiveController] Started. Evaluating every "
            + EVALUATION_INTERVAL_SEC + "s.");
    }

    /**
     * Stop the evaluation loop cleanly.
     * Waits up to 2 seconds for any in-progress evaluation to finish.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[AdaptiveController] Stopped.");
    }

    // -------------------------------------------------------------------------
    // Core evaluation logic
    // -------------------------------------------------------------------------

    /**
     * Called every EVALUATION_INTERVAL_SEC seconds by the scheduler.
     *
     * Reads current throughput and latency, then applies exactly one
     * adjustment (reduce or increase) per cycle, or none if healthy.
     */
    private void evaluate() {
        double throughput  = metrics.getAverageThroughput(); // req/s
        double avgLatency  = metrics.getAverageLatency();    // ms

        System.out.printf(
            "[AdaptiveController] throughput=%.2f req/s | avgLatency=%.2f ms | currentLimit=%d%n",
            throughput, avgLatency, rateLimiter.getCurrentLimit()
        );

        // --- Priority 1: Overload detection (reduce) -------------------------
        if (throughput > HIGH_THROUGHPUT_THRESHOLD) {
            adjustLimit(-REDUCE_STEP, "HIGH throughput (" + throughput + " req/s)");
            return; // one action per cycle
        }

        if (avgLatency > HIGH_LATENCY_THRESHOLD) {
            adjustLimit(-REDUCE_STEP, "HIGH latency (" + avgLatency + " ms)");
            return; // one action per cycle
        }

        // --- Priority 2: Underutilization detection (increase) ---------------
        if (throughput < LOW_THROUGHPUT_THRESHOLD) {
            adjustLimit(+INCREASE_STEP, "LOW throughput (" + throughput + " req/s)");
            return;
        }

        // --- Healthy — no change needed --------------------------------------
        System.out.println("[AdaptiveController] System healthy. No adjustment.");
    }

    /**
     * Applies a delta to the current token limit, clamping within
     * [MIN_TOKENS, MAX_TOKENS].
     *
     * @param delta   positive = increase, negative = reduce
     * @param reason  human-readable reason logged alongside the change
     */
    private void adjustLimit(int delta, String reason) {
        int current  = rateLimiter.getCurrentLimit();
        int proposed = current + delta;
        int clamped  = Math.max(MIN_TOKENS, Math.min(MAX_TOKENS, proposed));

        rateLimiter.setLimit(clamped);

        String direction = delta > 0 ? "INCREASED" : "REDUCED";
        System.out.printf(
            "[AdaptiveController] Limit %s: %d → %d | Reason: %s%n",
            direction, current, clamped, reason
        );
    }
}