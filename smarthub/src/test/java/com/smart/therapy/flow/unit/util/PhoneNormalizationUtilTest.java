package com.smart.therapy.flow.unit.util;

import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PhoneNormalizationUtil Unit Tests")
class PhoneNormalizationUtilTest {

    @Test
    void shouldNormalizeTenDigitNanpNumber() {
        assertThat(PhoneNormalizationUtil.normalizePhoneE164("5195551234")).isEqualTo("+15195551234");
        assertThat(PhoneNormalizationUtil.normalizePhoneE164("(519) 555-1234")).isEqualTo("+15195551234");
    }

    @Test
    void shouldNormalizeElevenDigitNumberStartingWithOne() {
        assertThat(PhoneNormalizationUtil.normalizePhoneE164("15195551234")).isEqualTo("+15195551234");
    }

    @Test
    void shouldNormalizePlusPrefixedInternationalNumber() {
        assertThat(PhoneNormalizationUtil.normalizePhoneE164("+44 7911 123456")).isEqualTo("+447911123456");
    }

    @Test
    void shouldReturnNullForAmbiguousNumber() {
        assertThat(PhoneNormalizationUtil.normalizePhoneE164("12345")).isNull();
        assertThat(PhoneNormalizationUtil.normalizePhoneE164("")).isNull();
        assertThat(PhoneNormalizationUtil.normalizePhoneE164(null)).isNull();
    }
}
