package com.lifehub.serviceregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/** The Eureka server that LifeHub services register with and lifehub-app discovers them through. */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {

    /** Starts the lifehub-service-registry service. */
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
