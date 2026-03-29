package executor;

import limiter.RateLimiter;
import metrics.MetricsCollector;
import model.Request;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestHandler {

    private final RateLimiter rateLimiter;
    private final MetricsCollector metrics;
    private final ExecutorService executor;

    private static final String[] USER_IDS  = {"user-1", "user-2", "user-3"};
    private static final String[] REQ_TYPES = {"READ", "WRITE", "DELETE"};

    private final AtomicInteger requestCounter = new AtomicInteger(0);

    public RequestHandler(RateLimiter rateLimiter, MetricsCollector metrics, int threadPoolSize) {
        this.rateLimiter = rateLimiter;
        this.metrics     = metrics;
        this.executor    = Executors.newFixedThreadPool(threadPoolSize);
    }

    public void simulateRequests(int totalRequests) {
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(this::handleRequest);
        }
    }

    private void handleRequest() {
        int count = requestCounter.getAndIncrement();

        String userId = USER_IDS[count % USER_IDS.length];
        String type   = REQ_TYPES[count % REQ_TYPES.length];

        Request request = new Request("req-" + count, userId, type);

        boolean allowed = rateLimiter.allowRequest(request);

        if (allowed) {
            long start = System.currentTimeMillis();

            simulateProcessing();

            long latency = System.currentTimeMillis() - start;

            metrics.recordLatency(latency);

            System.out.println("[ALLOWED]  " + request + " | latency=" + latency + "ms");
        } else {
            System.out.println("[REJECTED] " + request);
        }
    }

    private void simulateProcessing() {
        try {
            Thread.sleep(50); // simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}