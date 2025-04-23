package com.zzergatstage;

import com.zzergatstage.kafkatests.LoanRequestProducer;
import com.zzergatstage.service.LoanGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class LoanKeeperApplication implements CommandLineRunner {

    private final LoanGeneratorService generatorService;
    private final LoanRequestProducer producer;

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(LoanKeeperApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        generatorService.generateAndSendLoans();
    }
}