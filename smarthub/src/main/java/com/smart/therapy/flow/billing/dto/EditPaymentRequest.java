package com.smart.therapy.flow.billing.dto;

import com.smart.therapy.flow.billing.enums.PaymentMethod;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class EditPaymentRequest {
    @Positive
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private Instant paymentDate;
    private String reference;
    private String notes;
}
