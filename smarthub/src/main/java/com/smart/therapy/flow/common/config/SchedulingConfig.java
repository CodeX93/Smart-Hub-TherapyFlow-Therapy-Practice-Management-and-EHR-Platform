package com.smart.therapy.flow.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "clienthub.migration.enabled", havingValue = "false", matchIfMissing = true)
public class SchedulingConfig {
}
