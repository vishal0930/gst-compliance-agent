package com.gstcompliance;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class GstComplianceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GstComplianceApplication.class, args);
    }

    @Bean
    CommandLineRunner test(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String user,
            @Value("${spring.datasource.password}") String pass
    ) {
        return args -> {
            System.out.println("=================================");
            System.out.println("URL = " + url);
            System.out.println("USER = " + user);
            System.out.println("PASS = " + pass);
            System.out.println("=================================");
        };
    }
}