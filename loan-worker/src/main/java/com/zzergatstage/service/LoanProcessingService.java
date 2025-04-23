package com.zzergatstage.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzergatstage.model.LoanRequest;
import com.zzergatstage.model.LoanResult;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for processing loan requests and publishing results.
 */
@Service
@RequiredArgsConstructor
public class LoanProcessingService {

    private static final String RESULT_TOPIC = "loan.results";

    private final KafkaTemplate<String,String> kafkaTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Handles incoming loan request messages by processing them
     * and sending the result to Kafka.
     *
     * @param message JSON string representing the loan request.
     */
    public void handleLoanRequest(String message) {
        try {
            LoanRequest request = mapper.readValue(message, LoanRequest.class);

            // Simulate processing logic
            LoanResult result = process(request);

            String resultJson = mapper.writeValueAsString(result);
            kafkaTemplate.send(RESULT_TOPIC, resultJson);
        } catch (Exception e) {
            e.printStackTrace();  // In production, use proper logging
        }
    }

    /**
     * Simulates loan processing.
     *
     * @param request The loan request.
     * @return The loan result.
     */
    private LoanResult process(LoanRequest request) {
        // Mock processing logic
        boolean approved = request.amount() <= 5000;
        return new LoanResult(request.loanId(), approved ? "APPROVED" : "REJECTED");
    }
}
