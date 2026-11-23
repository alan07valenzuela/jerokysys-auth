package com.jerokysys.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.jerokysys.auth",
        "com.jerokysys.commons.exception"
})
@EnableDiscoveryClient
public class JerokysysAuthApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(JerokysysAuthApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(JerokysysAuthApplication.class, args);
    }

}