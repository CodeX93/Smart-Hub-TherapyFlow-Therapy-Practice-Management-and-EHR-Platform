package com.smart.therapy.flow.consultation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.consultation")
public class ConsultationProperties {

    /** Ops copy email on public consultation bookings. */
    private String opsEmail = "mail@resiliencec.com";

    private String serviceCode = "CONSULTATION";
}
