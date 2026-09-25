package com.smart.therapy.flow.system.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "practice_configuration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class PracticeConfiguration extends BaseEntity {

    @Column(name = "practice_name", nullable = false, length = 255)
    private String practiceName;

    @Column(name = "practice_address", columnDefinition = "TEXT")
    private String practiceAddress;

    @Column(name = "practice_phone", length = 50)
    private String practicePhone;

    @Column(name = "practice_email", length = 255)
    private String practiceEmail;

    @Column(name = "practice_website", length = 255)
    private String practiceWebsite;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "license_state", length = 50)
    private String licenseState;

    @Column(name = "npi_number", length = 50)
    private String npiNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String subtitle;

    @Column(length = 50)
    private String timezone;
}
