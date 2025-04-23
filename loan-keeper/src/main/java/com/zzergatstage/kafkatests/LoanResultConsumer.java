package com.zzergatstage.kafkatests;

import com.zzergatstage.model.LoanResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka consumer that listens for loan processing results and counts them.
 */
@Component
public class LoanResultConsumer {

    private final AtomicInteger processedCount = new AtomicInteger(0);

    @Value("${loan.generator.count:100}")
    private int expectedCount;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Consumes loan results from Kafka and tracks progress.
     *
     * @param message JSON-formatted loan result.
     */
    @KafkaListener(topics = "loan.results", groupId = "loan-keeper-group")
    public void consumeLoanResult(String message) {
        try {
            LoanResult result = mapper.readValue(message, LoanResult.class);
            int count = processedCount.incrementAndGet();
            System.out.printf("[INFO] Processed loan %s - Status: %s (%d/%d)%n",
                    result.loanId(), result.status(), count, expectedCount);

            if (count >= expectedCount) {
                System.out.println("[INFO] All loans processed. Shutting down.");
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
