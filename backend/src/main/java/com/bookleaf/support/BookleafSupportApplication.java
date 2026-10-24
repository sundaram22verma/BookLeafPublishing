package com.bookleaf.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // SSE heartbeat
@EnableAsync // AI triage runs off the request thread so ticket creation never waits on the LLM
public class BookleafSupportApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookleafSupportApplication.class, args);
        System.out.println("Bookleaf Support Application started successfully.");
    }
}