package com.smart.therapy.flow.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileEducationResponse {
    private Long id;
    private String degreeType;
    private String fieldOfStudy;
    private String institution;
    private Integer graduationYear;
    private LocalDate graduationDate;
    private Boolean isAccredited;
    private String accreditationBody;
    private Integer displayOrder;
    private String notes;
}

