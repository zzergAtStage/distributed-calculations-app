package com.zzergatstage.consumer;

import com.zzergatstage.model.CalculationJob;
import com.zzergatstage.model.CalculationJobRepository;
import com.zzergatstage.model.CalculationTask;
import com.zzergatstage.model.CalculationTaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author father
 */
@Component
@Order(10)
public class MockDataLoader implements CommandLineRunner {

    private final CalculationJobRepository jobRepo;
    private final CalculationTaskRepository taskRepo;

    public MockDataLoader(CalculationJobRepository jobRepo, CalculationTaskRepository taskRepo) {
        this.jobRepo = jobRepo;
        this.taskRepo = taskRepo;
    }

    @Override
    public void run(String... args) {
        for (int i = 1; i <= 5; i++) {
            CalculationJob job = new CalculationJob();
            job.setJobName("Job " + i);
            job = jobRepo.save(job);

            for (int j = 1; j <= 3; j++) {
                CalculationTask task = new CalculationTask();
                task.setTaskName("Task " + j + " of Job " + i);
                task.setJob(job);
                taskRepo.save(task);
            }
        }
    }
}
