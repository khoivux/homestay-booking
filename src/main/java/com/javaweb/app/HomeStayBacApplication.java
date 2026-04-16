package com.javaweb.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomeStayBacApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeStayBacApplication.class, args);
    }

}
