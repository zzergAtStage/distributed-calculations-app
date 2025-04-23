package com.zzergatstage.service;

import com.zzergatstage.domain.Loan;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Worker task that continuously pulls loans from the queue
 */
public class LoanWorker implements Runnable {
    private final BlockingQueue<Loan> loanQueue;
    private final AtomicLong processedCounter;

    public LoanWorker(BlockingQueue<Loan> loanQueue, AtomicLong processedCounter) {
        this.loanQueue = loanQueue;
        this.processedCounter = processedCounter;
    }

    /**
     * Continuously poll the queue until it's empty
     */
    @Override
    public void run() {
        try {
            while (true) {
                Loan loan = loanQueue.poll(1, TimeUnit.SECONDS);
                if (loan == null) {
                    break; // Queue is empty for 1 sec -> done
                }
                processLoan(loan);
                processedCounter.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simulates loan processing with variable delay
     */
    private void processLoan(Loan loan) {
        try {
            // Simulate variable work time per loan
            long delay = (long) (Math.random() * 100); // 0-100 ms
            Thread.sleep(delay);
            // System.out.println("Processed loan ID: " + loan.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
