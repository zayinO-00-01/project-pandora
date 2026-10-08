package com.projectpandora.api.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class BusinessDateConfig {
    @Bean
    LocalValidatorFactoryBean validator() {
        var validator = new LocalValidatorFactoryBean();
        validator.setConfigurationInitializer(config ->
            config.clockProvider(() -> Clock.system(ZoneId.of("Asia/Shanghai"))));
        return validator;
    }
}
