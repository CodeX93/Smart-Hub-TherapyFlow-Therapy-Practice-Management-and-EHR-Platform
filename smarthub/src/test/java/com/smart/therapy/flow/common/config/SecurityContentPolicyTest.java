package com.smart.therapy.flow.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContentPolicyTest {

    @Test
    void contentSecurityPolicyDoesNotAllowInlineOrEvalExecution() {
        assertThat(SecurityConfig.CONTENT_SECURITY_POLICY)
                .contains("script-src 'self'", "style-src 'self'", "connect-src 'self'", "object-src 'none'")
                .doesNotContain("'unsafe-inline'", "'unsafe-eval'", "connect-src 'self' https:");
    }
}
