package com.smart.therapy.flow.superadmin.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PlanEntitlementsImportResponse {
    PlanEntitlementsResponse updated;
    List<ImportError> errors;
    Integer total;

    @Value
    @Builder
    public static class ImportError {
        Integer line;
        String error;
    }
}
