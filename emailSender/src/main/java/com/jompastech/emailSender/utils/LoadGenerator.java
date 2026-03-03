package com.jompastech.emailSender.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

/**
 * A load generator that sends real HTTP POST requests to the users endpoint.
 * It simulates both successful and failing requests based on predefined payloads.
 * The generator runs concurrently using multiple threads and includes random delays
 * between requests to mimic realistic traffic.
 */
public class LoadGenerator {

    // ======================== CONFIGURATION ========================
    /** Target URL for the POST requests. */
    private static final String TARGET_URL = "http://localhost:8081/users";

    /** Number of concurrent threads (virtual users). */
    private static final int THREAD_COUNT = 5;

    /** Total number of requests to send across all threads. */
    private static final int TOTAL_REQUESTS = 100;

    /** Probability (0.0 to 1.0) of sending a failing request. */
    private static final double FAILURE_RATIO = 0.1; // 10% failures

    /** Minimum delay in milliseconds between requests sent by a single thread. */
    private static final int MIN_DELAY_MS = 500;

    /** Maximum delay in milliseconds between requests sent by a single thread. */
    private static final int MAX_DELAY_MS = 2000;

    /** Connection and request timeout in seconds. */
    private static final int TIMEOUT_SECONDS = 10;

    // ======================== PAYLOAD TEMPLATES ========================
    /** JSON payload for a successful request. */
    private static final String SUCCESS_PAYLOAD = """
            {
                "name": "Joao",
                "email": "joao@test.com"
            }
            """;

    /** JSON payload for a failing request. */
    private static final String FAILURE_PAYLOAD = """
            {
                "name": "Test Fail",
                "email": "fail@test.com"
            }
            """;

    // ======================== STATISTICS ========================
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0); // network or unexpected errors

    private static final Random random = new Random();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        System.out.println("Starting load generator...");
        System.out.printf("Target URL: %s%n", TARGET_URL);
        System.out.printf("Threads: %d, Total requests: %d, Failure ratio: %.2f%n",
                THREAD_COUNT, TOTAL_REQUESTS, FAILURE_RATIO);
        System.out.printf("Delay between requests: %d-%d ms%n", MIN_DELAY_MS, MAX_DELAY_MS);

        // Create an HttpClient with a reasonable timeout
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        // Thread pool for concurrent execution
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // Calculate requests per thread (distribute evenly)
        int requestsPerThread = TOTAL_REQUESTS / THREAD_COUNT;
        int remainder = TOTAL_REQUESTS % THREAD_COUNT;

        long startTime = System.currentTimeMillis();

        // Submit tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadRequests = requestsPerThread + (i < remainder ? 1 : 0);
            executor.submit(new RequestTask(client, threadRequests, i + 1));
        }

        // Shutdown executor and wait for all tasks to complete
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Load generator interrupted.");
        }

        long endTime = System.currentTimeMillis();
        long durationSeconds = (endTime - startTime) / 1000;

        // Print final statistics
        System.out.println("\n=== Load Generator Finished ===");
        System.out.printf("Total requests sent: %d%n", TOTAL_REQUESTS);
        System.out.printf("Successful (HTTP 2xx): %d%n", successCount.get());
        System.out.printf("Expected failures (HTTP 4xx/5xx): %d%n", failureCount.get());
        System.out.printf("Network/other errors: %d%n", errorCount.get());
        System.out.printf("Total time: %d seconds%n", durationSeconds);
    }

    /**
     * A task that sends a specified number of HTTP POST requests.
     * Each request includes a random delay before sending to simulate real user behavior.
     * The payload (success or failure) is chosen randomly based on the configured ratio.
     */
    private static class RequestTask implements Runnable {
        private final HttpClient client;
        private final int requestCount;
        private final int threadId;

        /**
         * Constructs a new RequestTask.
         *
         * @param client       the HttpClient to use for sending requests
         * @param requestCount the number of requests this task will send
         * @param threadId     an identifier for the thread (used for logging)
         */
        public RequestTask(HttpClient client, int requestCount, int threadId) {
            this.client = client;
            this.requestCount = requestCount;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            for (int i = 0; i < requestCount; i++) {
                // Choose payload based on failure ratio
                String payload = random.nextDouble() < FAILURE_RATIO ? FAILURE_PAYLOAD : SUCCESS_PAYLOAD;

                try {
                    // Random delay before sending the request
                    int delayMs = MIN_DELAY_MS + random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
                    Thread.sleep(delayMs);

                    // Build and send the request
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(TARGET_URL))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // Classify the response
                    int statusCode = response.statusCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        successCount.incrementAndGet();
                        logRequest(true, statusCode, threadId, i + 1);
                    } else {
                        failureCount.incrementAndGet();
                        logRequest(false, statusCode, threadId, i + 1);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.printf("[Thread %d] Interrupted.%n", threadId);
                    break;
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.printf("[Thread %d] Request error: %s%n", threadId, e.getMessage());
                }
            }
        }

        /**
         * Logs a single request result.
         *
         * @param success    true if the request was successful (HTTP 2xx), false otherwise
         * @param statusCode the HTTP status code received
         * @param threadId   the thread that sent the request
         * @param requestNum the sequential number of the request within this thread
         */
        private void logRequest(boolean success, int statusCode, int threadId, int requestNum) {
            String status = success ? "SUCCESS" : "FAILURE";
            System.out.printf("[Thread %d] Request %d: %s (HTTP %d)%n",
                    threadId, requestNum, status, statusCode);
        }
    }
}