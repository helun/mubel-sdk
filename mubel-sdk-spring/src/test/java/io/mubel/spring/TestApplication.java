package io.mubel.spring;

import io.mubel.sdk.scheduled.ScheduledEventsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Bean
    public ScheduledEventsConfig<String> scheduledEventsConfig() {
        return ScheduledEventsConfig.forAllCategories(
                System.out::println,
                String.class
        );
    }

}
