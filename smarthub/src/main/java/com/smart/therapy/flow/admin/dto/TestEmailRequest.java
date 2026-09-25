package com.smart.therapy.flow.admin.dto;

@io.swagger.v3.oas.annotations.media.Schema(description = "Request to send test email")
public class TestEmailRequest {
    @io.swagger.v3.oas.annotations.media.Schema(description = "Email address to send test email to (REQUIRED)", example = "test@example.com", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
    private String toEmail;

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }
}
