package com.lifehub.planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlannerApplication {

    /** Starts the planner service. */
    public static void main(String[] args) {
        SpringApplication.run(PlannerApplication.class, args);
    }
}
