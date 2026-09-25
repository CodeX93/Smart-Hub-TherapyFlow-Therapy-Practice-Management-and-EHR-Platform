package com.smart.therapy.flow.notification.controller;

import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.notification.service.SmsInboundService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@Hidden
public class TwilioInboundSmsController {

    private final SmsInboundService smsInboundService;

    @PostMapping(value = "/inbound", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleInbound(
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature,
            HttpServletRequest request) {
        String requestUrl = buildRequestUrl(request);
        Map<String, String> normalizedParams = new LinkedHashMap<>(params);
        SmsInboundService.InboundResult result = smsInboundService.handleInbound(
                signature,
                requestUrl,
                normalizedParams,
                HttpRequestUtil.getClientIp(request));
        return ResponseEntity.status(result.statusCode())
                .contentType(MediaType.APPLICATION_XML)
                .body(result.body());
    }

    private String buildRequestUrl(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            url.append('?').append(query);
        }
        return url.toString();
    }
}
