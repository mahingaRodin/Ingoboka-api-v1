package com.ingoboka_api.v1.ussd.controllers;

import com.ingoboka_api.v1.ussd.services.UssdOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Africa's Talking USSD callback adapter.
 *
 * <p>Configure AT sandbox to POST here. Service code demo: {@code *477#}.
 * Business logic lives in {@link UssdOrchestrator}, not in this controller.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ussd")
@RequiredArgsConstructor
@Tag(name = "USSD", description = "Africa's Talking USSD sandbox adapter (*477#) — DEMO/SANDBOX")
public class UssdGatewayController {

    private final UssdOrchestrator ussdOrchestrator;

    @Value("${ingoboka.ussd.enabled:true}")
    private boolean ussdEnabled;

    @Value("${ingoboka.ussd.gateway-api-key:}")
    private String gatewayApiKey;

    @Value("${ingoboka.ussd.service-code:*477#}")
    private String configuredServiceCode;

    @PostMapping(
            value = "/callback",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Africa's Talking USSD callback", description = "Returns CON/END plain text")
    public String callback(
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "serviceCode", required = false) String serviceCode,
            @RequestParam(value = "text", required = false) String text,
            @RequestHeader(value = "X-Ussd-Api-Key", required = false) String apiKey) {
        if (!ussdEnabled) {
            return "END USSD disabled";
        }
        if (StringUtils.hasText(gatewayApiKey) && !gatewayApiKey.equals(apiKey)) {
            log.warn("Rejected USSD callback: invalid API key");
            return "END Unauthorized";
        }
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(phoneNumber)) {
            return "END Invalid USSD request";
        }

        String code = StringUtils.hasText(serviceCode) ? serviceCode : configuredServiceCode;
        log.info(
                "USSD callback session={} phone={} code={} text={}",
                sessionId,
                phoneNumber,
                code,
                text);
        return ussdOrchestrator.handle(sessionId, phoneNumber, code, text != null ? text : "");
    }

    /** Local simulator — same contract as AT, easier for Postman demos. */
    @PostMapping(value = "/simulate", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Simulate USSD dial (local/demo)", description = "JSON-friendly alternative to form POST")
    public String simulate(
            @RequestParam String sessionId,
            @RequestParam String phoneNumber,
            @RequestParam(required = false, defaultValue = "") String text,
            @RequestParam(required = false) String serviceCode,
            @RequestHeader(value = "X-Ussd-Api-Key", required = false) String apiKey) {
        return callback(
                sessionId,
                phoneNumber,
                serviceCode != null ? serviceCode : configuredServiceCode,
                text,
                apiKey);
    }
}
