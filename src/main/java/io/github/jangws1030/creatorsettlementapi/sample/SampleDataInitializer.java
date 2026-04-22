package io.github.jangws1030.creatorsettlementapi.sample;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleDataInitializer {

    @Bean
    public ApplicationRunner sampleDataRunner(
            SampleDataService sampleDataService,
            @Value("${app.seed.enabled:true}") boolean seedEnabled
    ) {
        return args -> {
            if (seedEnabled) {
                sampleDataService.seedDefaultDataIfEmpty();
            }
        };
    }
}
