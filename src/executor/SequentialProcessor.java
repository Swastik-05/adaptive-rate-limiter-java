package executor;

import limiter.RateLimiter;
import metrics.MetricsCollector;
import model.Request;

public class SequentialProcessor {

    private final RateLimiter rateLimiter;
    private final MetricsCollector metrics;

    private static final String[] USER_IDS  = {"user-1", "user-2", "user-3"};
    private static final String[] REQ_TYPES = {"READ", "WRITE", "DELETE"};

    public SequentialProcessor(RateLimiter rateLimiter, MetricsCollector metrics) {
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
    }

    public long processRequests(int totalRequests) {

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {

            String userId = USER_IDS[i % USER_IDS.length];
            String type   = REQ_TYPES[i % REQ_TYPES.length];

            Request request = new Request("seq-" + i, userId, type);

            boolean allowed = rateLimiter.allowRequest(request);

            if (allowed) {
                long start = System.currentTimeMillis();

                simulateProcessing();

                long latency = System.currentTimeMillis() - start;
                metrics.recordLatency(latency);
            }
        }

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private void simulateProcessing() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}