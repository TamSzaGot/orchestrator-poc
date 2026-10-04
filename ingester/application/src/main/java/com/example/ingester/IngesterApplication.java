package com.example.ingester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.example.ingester")
@ConfigurationPropertiesScan(basePackages = "com.example.ingester")
public class IngesterApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngesterApplication.class, args);
    }
}
