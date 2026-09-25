package com.smart.therapy.flow.billing.audit;

/**
 * Canonical audit action names for billing, payments, and Stripe Connect flows.
 */
public final class BillingAuditActions {

    private BillingAuditActions() {
    }

    public static final String BILLING_CREATED = "billing_created";
    public static final String BILLING_STATUS_CHANGED = "billing_status_changed";
    public static final String PAYMENT_RECORDED = "payment_recorded";
    public static final String PAYMENT_VOIDED = "payment_voided";
    public static final String DISCOUNT_APPLIED = "discount_applied";
    public static final String INVOICE_SENT = "invoice_sent";
    public static final String INVOICE_VIEWED = "invoice_viewed";
    public static final String INVOICES_VIEWED = "invoices_viewed";
    public static final String INVOICE_DOWNLOADED = "invoice_downloaded";
    public static final String INVOICE_PDF_VIEWED = "invoice_pdf_viewed";
    public static final String PAYMENT_INITIATED = "payment_initiated";
    public static final String PAYMENT_COMPLETED = "payment_completed";
    public static final String STRIPE_ONBOARDING_STARTED = "stripe_onboarding_started";
    public static final String STRIPE_ONBOARDING_COMPLETED = "stripe_onboarding_completed";
    public static final String STRIPE_ONBOARDING_STATUS_CHANGED = "stripe_onboarding_status_changed";
    public static final String STRIPE_WEBHOOK_PROCESSED = "stripe_webhook_processed";
}
