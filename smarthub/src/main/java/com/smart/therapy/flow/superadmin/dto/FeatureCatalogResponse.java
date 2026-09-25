package com.smart.therapy.flow.superadmin.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class FeatureCatalogResponse {
    String id;
    String key;
    String name;
    String description;
    String scope;
    String type;
    Boolean defaultEnabled;
    Boolean deprecated;
    Instant deprecatedAt;
    Instant createdAt;
}
