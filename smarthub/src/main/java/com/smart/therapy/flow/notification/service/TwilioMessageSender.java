package com.smart.therapy.flow.notification.service;

/**
 * Test seam for sending messages through Twilio without static SDK calls.
 */
@FunctionalInterface
public interface TwilioMessageSender {
    String send(String toE164, String fromE164, String body) throws Exception;
}
