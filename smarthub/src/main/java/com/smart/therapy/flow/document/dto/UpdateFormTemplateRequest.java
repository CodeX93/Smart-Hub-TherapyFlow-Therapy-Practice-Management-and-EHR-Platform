package com.smart.therapy.flow.document.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;
import com.smart.therapy.flow.common.jackson.PatchAwareRequestDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = PatchAwareRequestDeserializer.class)
@Schema(description = "Request to update an existing form template")
public class UpdateFormTemplateRequest extends PatchAwareRequest {

    @Schema(description = "Form template name", example = "Intake Form")
    private String name;

    @Schema(description = "Form template description", example = "Initial intake form for new clients")
    private String description;

    @Schema(description = "Form category", example = "intake", allowableValues = {"consent", "intake", "release", "agreement", "safety", "discharge", "custom"})
    private String category;

    @Schema(description = "Instructions for filling out the form")
    private String instructions;

    @Schema(description = "Whether the form requires a signature", example = "true")
    private Boolean requiresSignature;

    @Schema(description = "Whether the template is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Whether this is a system template", example = "false")
    private Boolean isSystemTemplate;

    @Schema(description = "Sort order for display", example = "1")
    private Integer sortOrder;

    @Schema(description = "List of form fields. null=clone prior structure, []=replace with zero fields.", example = "[]")
    private List<CreateFormFieldInTemplateRequest> fields;
}
