package com.ecommerce.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TeaRoomApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeaRoomApplication.class, args);
    }
}
