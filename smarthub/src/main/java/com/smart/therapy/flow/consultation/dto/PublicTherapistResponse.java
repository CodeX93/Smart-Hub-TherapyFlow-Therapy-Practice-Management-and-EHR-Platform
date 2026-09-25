package com.smart.therapy.flow.consultation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicTherapistResponse {
    private Long id;
    private String fullName;
    private String email;
    private String title;
    private String phone;
    private Boolean isActive;
}
