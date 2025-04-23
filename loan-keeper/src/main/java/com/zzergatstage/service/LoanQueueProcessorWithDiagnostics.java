package com.zzergatstage.service;

import com.zzergatstage.domain.Loan;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main class to start the loan processing application with diagnostics
 */
public class LoanQueueProcessorWithDiagnostics {

    public static void main(String[] args) throws InterruptedException {
        final long totalLoans = 60_000_000L;
        final int numThreads = 100;

        System.out.printf("Initializing %d loans...%n", totalLoans);
        BlockingQueue<Loan> loanQueue = new LinkedBlockingQueue<>();
        for (long i = 0; i < totalLoans; i++) {
            loanQueue.add(new Loan(i));
        }

        System.out.println("Starting loan processing with " + numThreads + " threads...");
        AtomicLong processedCounter = new AtomicLong(0);
        long startTimeMillis = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads + 1); // +1 for progress logger

        // Start progress logging thread
        executor.submit(new ProgressLogger(processedCounter, totalLoans, startTimeMillis));

        // Start processing threads
        for (int i = 0; i < numThreads; i++) {
            executor.submit(new LoanWorker(loanQueue, processedCounter));
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.HOURS); // Adjust as needed
        System.out.println("✅ All loans processed. Total time: " +
                (System.currentTimeMillis() - startTimeMillis) / 1000 + " seconds.");
    }
}