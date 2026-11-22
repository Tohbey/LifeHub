package com.lifehub.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotesApplication {

    /** Starts the notes service. */
    public static void main(String[] args) {
        SpringApplication.run(NotesApplication.class, args);
    }
}
