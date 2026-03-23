package com.urbanblack.paymentservice.controller;

import com.urbanblack.paymentservice.service.PaymentService;
import com.urbanblack.paymentservice.util.PayuHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class PayuWebhookController {

    private final PaymentService paymentService;

    @Value("${payu.key:}")
    private String payuKey;

    @Value("${payu.salt:}")
    private String payuSalt;

    @Value("${payu.verify-hash:true}")
    private boolean verifyHash;

    /**
     * Server-to-server webhook endpoint for PayU.
     * PayU commonly posts application/x-www-form-urlencoded.
     */
    @PostMapping(
            value = "/payu",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<String> payuWebhook(
            @RequestParam(required = false) MultiValueMap<String, String> formParams,
            @RequestBody(required = false) Map<String, Object> jsonBody) {

        Map<String, String> p = PayuPayloadMapper.toFlatStringMap(formParams, jsonBody);

        String status = p.getOrDefault("status", "");
        String txnid = p.getOrDefault("txnid", "");
        String amount = p.getOrDefault("amount", "");
        String productinfo = p.getOrDefault("productinfo", "");
        String firstname = p.getOrDefault("firstname", "");
        String email = p.getOrDefault("email", "");
        String hash = p.getOrDefault("hash", "");
        String mihpayid = p.getOrDefault("mihpayid", ""); // PayU payment id

        if (txnid.isBlank() || status.isBlank()) {
            return ResponseEntity.badRequest().body("Missing txnid/status");
        }

        if (verifyHash) {
            if (payuKey == null || payuKey.isBlank() || payuSalt == null || payuSalt.isBlank()) {
                log.error("[PayU] Hash verification enabled but PAYU_KEY/PAYU_SALT not set");
                return ResponseEntity.status(500).body("Server not configured");
            }

            // PayU reverse-hash verification format for response:
            // salt|status|||||||||||email|firstname|productinfo|amount|txnid|key
            String reverse = payuSalt + "|" + status + "|||||||||||" +
                    email + "|" + firstname + "|" + productinfo + "|" +
                    amount + "|" + txnid + "|" + payuKey;
            String expected = PayuHashUtil.sha512Hex(reverse);

            if (!expected.equalsIgnoreCase(hash)) {
                log.warn("[PayU] Webhook hash mismatch txnid={}", txnid);
                return ResponseEntity.status(400).body("Invalid hash");
            }
        }

        boolean success = "success".equalsIgnoreCase(status);

        // Idempotent settlement: confirmByTxnId calls confirm(), which is idempotent.
        paymentService.confirmByTxnId(txnid, mihpayid, success);

        return ResponseEntity.ok("OK");
    }

    /**
     * Lightweight mapper because PayU can send form-encoded.
     */
    static final class PayuPayloadMapper {
        private PayuPayloadMapper() {}

        static Map<String, String> toFlatStringMap(MultiValueMap<String, String> formParams,
                                                   Map<String, Object> jsonBody) {
            if (formParams != null && !formParams.isEmpty()) {
                return formParams.toSingleValueMap();
            }
            if (jsonBody == null || jsonBody.isEmpty()) {
                return Map.of();
            }
            return jsonBody.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue() == null ? "" : String.valueOf(e.getValue()),
                            (a, b) -> a
                    ));
        }
    }
}

