package com.snip.link;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SnipLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnipLinkApplication.class, args);
    }

}
