package com.medicare.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AiChatbotServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiChatbotServiceApplication.class, args);
    }
}
