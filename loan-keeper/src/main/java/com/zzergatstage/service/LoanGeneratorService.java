package com.zzergatstage.service;

import com.zzergatstage.model.LoanRequest;
import com.zzergatstage.kafkatests.LoanRequestProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service responsible for generating loan requests and dispatching them to Kafka.
 */
@Service
@RequiredArgsConstructor
public class LoanGeneratorService {


    private final LoanRequestProducer producer;

    @Value("${loan.generator.count:100}")
    private int loanCount;

    /**
     * Generates a predefined number of loan requests and sends them via Kafka producer.
     */
    public void generateAndSendLoans() {
        for (int i = 1; i <= loanCount; i++) {
            LoanRequest request = new LoanRequest(
                    UUID.randomUUID().toString(),
                    "Applicant-" + i,
                    ThreadLocalRandom.current().nextDouble(1000, 10000)
            );
            producer.sendLoanRequest(request);
        }
        System.out.printf("[INFO] Generated and sent %d loan requests.%n", loanCount);
    }
}
