import executor.RequestHandler;
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

        // --- Initialize ---
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

        // --- Run Simulation ---
        System.out.println("\n=== Simulation Start ===\n");

        handler.simulateRequests(totalRequests);
        handler.shutdown();

        // --- Output ---
        System.out.println("\n=== Simulation Complete ===");
        System.out.println(metrics.getSummary());

        scanner.close();
    }
}