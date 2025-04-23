package com.zzergatstage.service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Progress reporter thread that logs real-time progress info
 */
public class ProgressLogger implements Runnable {
    private final AtomicLong processedCounter;
    private final long totalLoans;
    private final long startTimeMillis;

    public ProgressLogger(AtomicLong processedCounter, long totalLoans, long startTimeMillis) {
        this.processedCounter = processedCounter;
        this.totalLoans = totalLoans;
        this.startTimeMillis = startTimeMillis;
    }

    /**
     * Periodically logs the current progress and performance stats
     */
    @Override
    public void run() {
        try {
            while (true) {
                long processed = processedCounter.get();
                double percent = (100.0 * processed) / totalLoans;
                long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
                double loansPerSecond = (processed * 1000.0) / elapsedMillis;

                System.out.printf("[Progress] %,d / %,d loans processed (%.2f%%) | Speed: %.2f loans/sec | Elapsed: %d sec%n",
                        processed, totalLoans, percent, loansPerSecond, elapsedMillis / 1000);

                if (processed >= totalLoans) {
                    break;
                }

                Thread.sleep(5000); // Update every 5 seconds
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}