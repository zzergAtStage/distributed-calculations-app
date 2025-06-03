package com.zzergatstage.consumer;

import com.zzergatstage.model.CalculationJob;
import com.zzergatstage.model.CalculationJobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author father
 */
@Component
@Order(20)
public class QueryObserver implements CommandLineRunner {

    private final CalculationJobRepository jobRepo;

    public QueryObserver(CalculationJobRepository jobRepo) {
        this.jobRepo = jobRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(String... args) {
        System.out.println("=== Observing query counts ===");
        List<CalculationJob> jobs = jobRepo.findAll();

        for (CalculationJob job : jobs) {
            System.out.println(job.getJobName() + " has " + job.getTasks().size() + " tasks");
        }
        System.out.println("=== Done ===");
    }
}
