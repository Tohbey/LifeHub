package com.lifehub.identityworkspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IdentityWorkspaceApplication {

    /** Starts the identity-workspace service. */
    public static void main(String[] args) {
        SpringApplication.run(IdentityWorkspaceApplication.class, args);
    }
}
