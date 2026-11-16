package com.lifehub.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LifehubApplication {

    /** Starts lifehub-app: the PWA and the API gateway. */
    public static void main(String[] args) {
        SpringApplication.run(LifehubApplication.class, args);
    }
}
