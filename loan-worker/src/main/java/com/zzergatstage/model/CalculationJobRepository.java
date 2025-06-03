package com.zzergatstage.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author father
 */
@Repository
public interface CalculationJobRepository extends JpaRepository<CalculationJob, Long> {
    /**
     * Loads all CalculationJob entities and, using an EntityGraph,
     * fetches the "tasks" collection eagerly in one go.
     *
     * @return a List of CalculationJob, each with tasks loaded by EntityGraph.
     * <p>
     * Under the hood, this also triggers a JOIN similar to:
     *   SELECT j, t
     *     FROM CalculationJob j
     *     LEFT JOIN j.tasks t;
     * </p>
     */
/*     @EntityGraph(attributePaths = "tasks")
    List<CalculationJob> findAll();*/

    @Query("SELECT j FROM CalculationJob j LEFT JOIN FETCH j.tasks")
    List<CalculationJob> findAllWithTasks();
}
