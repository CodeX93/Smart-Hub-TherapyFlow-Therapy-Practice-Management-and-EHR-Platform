package com.smart.therapy.flow.unit.zoom;

import com.smart.therapy.flow.integration.zoom.ZoomOauthUrls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoomOauthUrlsTest {

    @Test
    void tokenUrl_fromHostOnly() {
        assertEquals("https://zoom.us/oauth/token", ZoomOauthUrls.tokenUrl("https://zoom.us"));
    }

    @Test
    void tokenUrl_stripsTrailingSlash() {
        assertEquals("https://zoom.us/oauth/token", ZoomOauthUrls.tokenUrl("https://zoom.us/"));
    }

    @Test
    void tokenUrl_stripsTrailingOauthPath() {
        assertEquals("https://zoom.us/oauth/token", ZoomOauthUrls.tokenUrl("https://zoom.us/oauth"));
        assertEquals("https://zoom.us/oauth/token", ZoomOauthUrls.tokenUrl("https://zoom.us/oauth/"));
    }

    @Test
    void tokenUrl_nullOrBlank_defaultsToZoomHost() {
        assertEquals("https://zoom.us/oauth/token", ZoomOauthUrls.tokenUrl(null));
        assertEquals("https://zoom.us/oauth/token", ZoomOauthUrls.tokenUrl("  "));
    }
}
