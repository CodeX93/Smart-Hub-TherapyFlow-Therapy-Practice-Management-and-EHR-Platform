package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalInvoiceResponse {
    private Long id;
    private Long sessionId;
    private String serviceCode;
    private String serviceName;
    private String sessionType;
    private String sessionMode;
    private Instant sessionDate;
    private Integer units;
    private BigDecimal ratePerUnit;
    private BigDecimal originalSubtotalAmount;
    private BigDecimal totalAmount;
    private Boolean insuranceCovered;
    private BigDecimal copayAmount;
    private LocalDate billingDate;
    private String paymentStatus;
    private String billingStatus;
    private BigDecimal paymentAmount;
    private BigDecimal amountDue;
    private BigDecimal outstandingAmount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private Instant createdAt;
}

