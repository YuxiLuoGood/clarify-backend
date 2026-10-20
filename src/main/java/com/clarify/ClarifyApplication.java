package com.clarify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

// Authentication is JWT-only, so Boot's default in-memory user (and its
// generated password in the logs) is switched off.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableAsync
public class ClarifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClarifyApplication.class, args);
    }
}
