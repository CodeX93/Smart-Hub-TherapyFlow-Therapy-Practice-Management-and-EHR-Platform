package com.smart.therapy.flow.client.dto;

import com.smart.therapy.flow.system.dto.OptionCategoryResponse;
import com.smart.therapy.flow.system.dto.SystemOptionResponse;
import com.smart.therapy.flow.task.dto.ChecklistTemplateResponse;
import com.smart.therapy.flow.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientFiltersBatchResponse {
    private List<UserResponse> therapists;
    private List<ChecklistTemplateResponse> checklistTemplates;
    private Map<String, SystemOptionCategory> systemOptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemOptionCategory {
        private OptionCategoryResponse category;
        private List<SystemOptionResponse> options;
    }
}
