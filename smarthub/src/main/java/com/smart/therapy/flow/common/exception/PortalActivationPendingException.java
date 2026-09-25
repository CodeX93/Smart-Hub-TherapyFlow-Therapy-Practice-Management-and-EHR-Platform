package com.smart.therapy.flow.common.exception;

/**
 * Thrown when a client tries to log in before completing portal activation.
 */
public class PortalActivationPendingException extends ForbiddenException {

    public PortalActivationPendingException() {
        super(ErrorCode.CLIENT_PORTAL_ACTIVATION_PENDING.getMessage());
    }
}
