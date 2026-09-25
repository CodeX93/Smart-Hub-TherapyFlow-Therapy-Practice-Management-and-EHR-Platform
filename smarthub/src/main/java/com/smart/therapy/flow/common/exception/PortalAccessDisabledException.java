package com.smart.therapy.flow.common.exception;

/**
 * Thrown when a client attempts portal login or API access while portal access is disabled.
 */
public class PortalAccessDisabledException extends ForbiddenException {

    public PortalAccessDisabledException() {
        super(ErrorCode.CLIENT_PORTAL_DISABLED.getMessage());
    }
}
