package com.lifehub.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class NotificationApplication {

    /** Starts the notification service. */
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
