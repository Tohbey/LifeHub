package com.lifehub.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuditApplication {

    /** Starts the audit service. */
    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}
