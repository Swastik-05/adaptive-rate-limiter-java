import executor.RequestHandler;
import executor.SequentialProcessor;
import limiter.RateLimiter;
import metrics.MetricsCollector;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // --- User Input ---
        System.out.print("Enter bucket capacity (tokens per user): ");
        int bucketCapacity = scanner.nextInt();

        System.out.print("Enter refill rate (tokens/sec): ");
        double refillRate = scanner.nextDouble();

        System.out.print("Enter thread pool size: ");
        int threadPoolSize = scanner.nextInt();

        System.out.print("Enter total number of requests: ");
        int totalRequests = scanner.nextInt();

        // ================================
        // 🔴 SEQUENTIAL EXECUTION
        // ================================
        System.out.println("\n=== Sequential Execution ===");

        MetricsCollector seqMetrics = new MetricsCollector();
        RateLimiter seqLimiter = new RateLimiter(bucketCapacity, refillRate, seqMetrics);

        SequentialProcessor sequentialProcessor =
                new SequentialProcessor(seqLimiter, seqMetrics);

        long seqStart = System.currentTimeMillis();
        long seqTime = sequentialProcessor.processRequests(totalRequests);
        long seqEnd = System.currentTimeMillis();

        System.out.println("Sequential Time: " + (seqEnd - seqStart) + " ms");
        System.out.println(seqMetrics.getSummary());

        // ================================
        // 🔵 MULTITHREADED EXECUTION (YOUR ORIGINAL SYSTEM)
        // ================================
        System.out.println("\n=== Multithreaded Execution ===\n");

        MetricsCollector metrics = new MetricsCollector();

        RateLimiter rateLimiter = new RateLimiter(
                bucketCapacity,
                refillRate,
                metrics
        );

        RequestHandler handler = new RequestHandler(
                rateLimiter,
                metrics,
                threadPoolSize
        );

        long parallelStart = System.currentTimeMillis();

        // 👉 This shows ALLOWED / REJECTED logs (important)
        handler.simulateRequests(totalRequests);
        handler.shutdown();

        long parallelEnd = System.currentTimeMillis();
        long parallelTime = parallelEnd - parallelStart;

        System.out.println("\n=== Simulation Complete ===");
        System.out.println("Multithreaded Time: " + parallelTime + " ms");
        System.out.println(metrics.getSummary());

        // ================================
        // 🟢 PERFORMANCE COMPARISON
        // ================================
        System.out.println("\n=== Performance Comparison ===");

        double improvement = (double) seqTime / parallelTime;

        System.out.println("Speed Improvement: " +
                String.format("%.2f", improvement) + "x faster");

        scanner.close();
    }
}