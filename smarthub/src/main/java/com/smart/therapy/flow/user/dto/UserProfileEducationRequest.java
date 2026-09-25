package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Education entry for user profile updates")
public class UserProfileEducationRequest {

    @Schema(description = "Degree type", example = "MS Clinical Psychology")
    private String degreeType;

    @Schema(description = "Field of study", example = "Clinical Psychology")
    private String fieldOfStudy;

    @Schema(description = "Institution name", example = "University of Karachi")
    private String institution;

    @Schema(description = "Graduation year", example = "2021")
    private Integer graduationYear;

    @Schema(description = "Graduation date", example = "2021-06-30", type = "string", format = "date")
    private LocalDate graduationDate;

    @Schema(description = "Whether institution is accredited", example = "true")
    private Boolean isAccredited;

    @Schema(description = "Accreditation body", example = "HEC")
    private String accreditationBody;

    @Schema(description = "Notes about this degree", example = "Focus on CBT and trauma-informed care")
    private String notes;
}

