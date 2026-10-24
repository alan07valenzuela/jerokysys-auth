package com.jerokysys.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.jerokysys.auth",
        "com.jerokysys.commons.exception"
})
@EnableDiscoveryClient
public class JerokysysAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(JerokysysAuthApplication.class, args);
    }

}
