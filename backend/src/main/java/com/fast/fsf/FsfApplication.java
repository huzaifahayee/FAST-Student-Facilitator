package com.fast.fsf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FSFApplication
 * 
 * What is this class?
 * This is the exact entry point of the entire FAST Student Facilitator backend.
 * When you run the project, Java looks for the `main` method here to start up.
 *
 * Educational Note: @SpringBootApplication
 * This is a "decorator" (called an Annotation in Java). By putting this above the class,
 * we tell the Spring framework: "Hey, this is a Spring Boot app. Please automatically
 * configure a web server (Tomcat) and scan this package for other controllers and services
 * so I don't have to wire them up manually."
 */
@SpringBootApplication
public class FsfApplication {

    public static void main(String[] args) {
        // This single line starts the Spring Boot framework, which in turn
        // starts up our REST APIs and connects to PostgreSQL.
        SpringApplication.run(FsfApplication.class, args);
    }
}
