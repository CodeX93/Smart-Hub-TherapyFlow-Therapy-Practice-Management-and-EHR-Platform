package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.service.EmailAppLinks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailAppLinks Unit Tests")
class EmailAppLinksTest {

    @Test
    @DisplayName("Absolute links repair relative and broken http:/// paths")
    void absoluteRepairsBrokenPaths() {
        String base = "https://app.therapyflow.pro";
        assertThat(EmailAppLinks.absolute(base, "/client-portal/invoices/2/pay"))
                .isEqualTo("https://app.therapyflow.pro/user/invoices");
        assertThat(EmailAppLinks.absolute(base, "http:///client-portal/invoices/2/pay"))
                .isEqualTo("https://app.therapyflow.pro/user/invoices");
        assertThat(EmailAppLinks.absolute(base, "https://calendar.google.com/calendar/render"))
                .isEqualTo("https://calendar.google.com/calendar/render");
    }

    @Test
    @DisplayName("Client recipient CTAs point at client invoices page")
    void clientRecipientUsesPortalInvoices() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentUrl", "/client-portal/invoices/2/pay");
        payload.put("invoiceUrl", "/client-portal/invoices/2");

        Map<String, Object> client = EmailAppLinks.forClientRecipient(payload, "https://app.therapyflow.pro/auth/staff/login");
        assertThat(client.get("paymentUrl")).isEqualTo("https://app.therapyflow.pro/user/invoices");
        assertThat(client.get("invoiceUrl")).isEqualTo("https://app.therapyflow.pro/user/invoices");
        assertThat(client.get("sessionUrl")).isEqualTo("https://app.therapyflow.pro/user/booked-sessions");
        assertThat(client.get("loginUrl")).isEqualTo("https://app.therapyflow.pro/auth/login");
    }

    @Test
    @DisplayName("Staff recipient CTAs point at staff portal")
    void staffRecipientUsesStaffPortal() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentUrl", "/client-portal/invoices/2/pay");

        Map<String, Object> staff = EmailAppLinks.forStaffRecipient(payload, "https://app.therapyflow.pro");
        assertThat(staff.get("paymentUrl")).isEqualTo("https://app.therapyflow.pro/auth/staff/login");
        assertThat(staff.get("invoiceUrl")).isEqualTo("https://app.therapyflow.pro/admin/billings");
        assertThat(staff.get("sessionUrl")).isEqualTo("https://app.therapyflow.pro/therapist/scheduling");
    }
}
