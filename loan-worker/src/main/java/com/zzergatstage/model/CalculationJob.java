package com.zzergatstage.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author father
 */
@Data
@Entity
public class CalculationJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobName;

    @OneToMany(mappedBy = "job")
    @Fetch(FetchMode.SUBSELECT)
    private List<CalculationTask> tasks = new ArrayList<>();
}
