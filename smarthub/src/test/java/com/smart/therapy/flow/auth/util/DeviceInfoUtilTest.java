package com.smart.therapy.flow.auth.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceInfoUtilTest {

    @Test
    void buildsReadableLabelFromUserAgent() {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
        assertThat(DeviceInfoUtil.deviceLabel(ua)).isEqualTo("Chrome on macOS");
    }

    @Test
    void fingerprintIsStableForSameAuthAndUaFamily() {
        String ua1 = "Mozilla/5.0 Chrome/126.0.0.0 Safari/537.36";
        String ua2 = "Mozilla/5.0 Chrome/126.0.0.0 Safari/537.36";
        assertThat(DeviceInfoUtil.fingerprintHash(7L, ua1))
                .isEqualTo(DeviceInfoUtil.fingerprintHash(7L, ua2));
        assertThat(DeviceInfoUtil.fingerprintHash(7L, ua1))
                .isNotEqualTo(DeviceInfoUtil.fingerprintHash(8L, ua1));
    }
}
