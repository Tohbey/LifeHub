package com.lifehub.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FinanceApplication {

    /** Starts the finance service. */
    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
