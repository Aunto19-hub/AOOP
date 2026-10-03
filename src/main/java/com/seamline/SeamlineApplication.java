package com.seamline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/** {@code @EnableAsync} lets emails (OTPs, approval notices) send off the request thread. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class SeamlineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeamlineApplication.class, args);
    }
}
