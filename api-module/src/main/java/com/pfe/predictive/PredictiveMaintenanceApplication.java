package com.pfe.predictive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.pfe.predictive", "com.yourpackage"})
@EnableScheduling
@EnableCaching
public class PredictiveMaintenanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PredictiveMaintenanceApplication.class, args);
    }
}
