package com.zzergatstage.consumer;

import com.zzergatstage.service.LoanProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LoanRequestConsumer {
    private final LoanProcessingService processingService;

    public LoanRequestConsumer(LoanProcessingService processingService) {
        this.processingService = processingService;
    }
    /**
     * Consumes loan requests from the 'loan.requests' Kafka topic.
     *
     * @param message JSON-formatted loan request.
     */
    @KafkaListener(topics = "loan.requests", groupId = "loan-worker-group")
    public void consumeLoanRequest(String message) {
        processingService.handleLoanRequest(message);
    }
}
