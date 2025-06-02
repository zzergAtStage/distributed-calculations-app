package com.zzergatstage.kafkatests;

import com.zzergatstage.model.LoanRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for publishing loan requests to the 'loan.requests' topic.
 */
@Component
public class LoanRequestProducer {

    private static final String REQUEST_TOPIC = "loan.requests";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sends a loan request to Kafka after serializing it to JSON.
     *
     * @param request The loan request to send.
     */
    public void sendLoanRequest(LoanRequest request) {
        try {
            String message = mapper.writeValueAsString(request);
            kafkaTemplate.send(REQUEST_TOPIC, request.loanId(), message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
