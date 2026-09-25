package com.smart.therapy.flow.auth.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {
    private final TotpService service = new TotpService();
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void matchesRfc6238Sha1Vectors() {
        assertThat(service.generateCode(RFC_SECRET, 59L / 30L, 8)).isEqualTo("94287082");
        assertThat(service.generateCode(RFC_SECRET, 1111111109L / 30L, 8)).isEqualTo("07081804");
        assertThat(service.generateCode(RFC_SECRET, 1111111111L / 30L, 8)).isEqualTo("14050471");
        assertThat(service.generateCode(RFC_SECRET, 1234567890L / 30L, 8)).isEqualTo("89005924");
        assertThat(service.generateCode(RFC_SECRET, 2000000000L / 30L, 8)).isEqualTo("69279037");
        assertThat(service.generateCode(RFC_SECRET, 20000000000L / 30L, 8)).isEqualTo("65353130");
    }

    @Test
    void acceptsAdjacentWindowAndPreventsReplay() {
        Instant now = Instant.ofEpochSecond(1_700_000_000L);
        long previousStep = now.getEpochSecond() / 30L - 1;
        String code = service.generateCode(RFC_SECRET, previousStep, 6);

        assertThat(service.verify(RFC_SECRET, code, now, 1, null)).isEqualTo(previousStep);
        assertThat(service.verify(RFC_SECRET, code, now, 1, previousStep)).isNull();
        assertThat(service.verify(RFC_SECRET, code, now, 0, null)).isNull();
    }

    @Test
    void generatesA160BitBase32Secret() {
        String secret = service.generateSecret();
        assertThat(secret).hasSize(32).matches("[A-Z2-7]+");
        assertThat(TotpService.decodeBase32(secret)).hasSize(20);
    }
}
